@file:Suppress("IllegalIdentifier")

package pl.elpassion.eltc.recap

import com.nhaarman.mockito_kotlin.*
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers.trampoline
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import pl.elpassion.eltc.Build
import pl.elpassion.eltc.Status
import pl.elpassion.eltc.api.TeamCityApi
import pl.elpassion.eltc.createBuild
import pl.elpassion.eltc.login.AuthData
import pl.elpassion.eltc.login.LoginRepository
import pl.elpassion.eltc.util.SchedulersSupplier
import pl.elpassion.eltc.util.logger
import pl.elpassion.eltc.util.testLogger
import java.util.*

class RecapControllerTest {

    private val ADDRESS = "http://teamcity"
    private val CREDENTIALS = "credentials"
    private val loginRepository = mock<LoginRepository>()
    private val recapRepository = mock<RecapRepository>()
    private val api = mock<TeamCityApi>()
    private val notifier = mock<RecapNotifier>()
    private val onFinish = mock<() -> Unit>()
    private val apiSubject = SingleSubject.create<List<Build>>()

    @Before
    fun setup() {
        logger = testLogger
        whenever(loginRepository.authData).thenReturn(AuthData(ADDRESS, CREDENTIALS))
        whenever(api.getFinishedBuilds(any())).thenReturn(apiSubject)
        whenever(api.getFinishedBuildsForProjects(any(), any())).thenReturn(apiSubject)
    }

    @Test
    fun `Set last finish date to initial date on first start`() {
        whenever(recapRepository.lastFinishDate).thenReturn(null)
        createController().onStart()
        verify(recapRepository).lastFinishDate = any()
    }

    @Test
    fun `Do not set last finish date to new date on subsequent start`() {
        whenever(recapRepository.lastFinishDate).thenReturn(Date())
        createController().onStart()
        verify(recapRepository, never()).lastFinishDate = anyOrNull()
    }

    @Test
    fun `Call api to get finished builds after last finish date`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2017)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        whenever(recapRepository.lastFinishDate).thenReturn(calendar.time)
        createController().onStart()
        verify(api).getFinishedBuilds(calendar.time)
    }

    @Test
    fun `Do not call api to get finished builds on first start`() {
        whenever(recapRepository.lastFinishDate).thenReturn(null)
        createController().onStart()
        verify(api, never()).getFinishedBuilds(any())
    }

    @Test
    fun `Call api to get finished builds only for selected projects when preferred`() {
        val projectsIds = listOf("Project1", "Project1")
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2017)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        whenever(recapRepository.lastFinishDate).thenReturn(calendar.time)
        createController(projectsIds = projectsIds).onStart()
        verify(api).getFinishedBuildsForProjects(calendar.time, projectsIds)
    }

    @Test
    fun `Update last finish date with new value from finished builds on api result`() {
        val lastFinishDate = Date(1502103373000)
        val newFinishDate = Date(1502103410000)
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        apiSubject.onSuccess(listOf(createBuild(finishDate = newFinishDate)))
        verify(recapRepository).lastFinishDate = newFinishDate
    }

    @Test
    fun `Update last finish date with max value from finished builds on api result`() {
        val lastFinishDate = Date(1502103373000)
        val newFinishDates = listOf(Date(1502103410000), Date(1502103410002), Date(1502103410001))
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        apiSubject.onSuccess(listOf(
                createBuild(finishDate = newFinishDates[0]),
                createBuild(finishDate = newFinishDates[1]),
                createBuild(finishDate = newFinishDates[2])))
        verify(recapRepository).lastFinishDate = newFinishDates[1]
    }

    @Test
    fun `Do not update last finish date on api error`() {
        val lastFinishDate = Date(1502103373000)
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        apiSubject.onError(RuntimeException())
        verify(recapRepository, never()).lastFinishDate = anyOrNull()
    }

    @Test
    fun `Do not update last finish date on empty result`() {
        val lastFinishDate = Date(1502103373000)
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        apiSubject.onSuccess(listOf(createBuild(finishDate = null)))
        verify(recapRepository, never()).lastFinishDate = anyOrNull()
    }

    @Test
    fun `Show notifications with failed builds on api result`() {
        val successfulBuild = createBuild(finishDate = Date(1502103410000), status = Status.SUCCESS)
        val failedBuild = createBuild(finishDate = Date(1502103410001), status = Status.FAILURE)
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController().onStart()
        apiSubject.onSuccess(listOf(successfulBuild, failedBuild))
        verify(notifier).showFailureNotifications(listOf(failedBuild))
    }

    @Test
    fun `Do not show notifications when no failures on api result`() {
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController().onStart()
        apiSubject.onSuccess(listOf(
                createBuild(finishDate = Date(1502103410000), status = Status.SUCCESS)))
        verify(notifier, never()).showFailureNotifications(any())
    }

    @Test
    fun `Invoke finish on first start`() {
        whenever(recapRepository.lastFinishDate).thenReturn(null)
        createController().onStart()
        verify(onFinish).invoke()
    }

    @Test
    fun `Invoke finish when auth data not available`() {
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        whenever(loginRepository.authData).thenReturn(null)
        createController().onStart()
        verify(onFinish).invoke()
    }

    @Test
    fun `Do not invoke finish on subsequent start before result from api`() {
        val lastFinishDate = Date(1502103373000)
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        verify(onFinish, never()).invoke()
    }

    @Test
    fun `Invoke finish on api error`() {
        val lastFinishDate = Date(1502103373000)
        whenever(recapRepository.lastFinishDate).thenReturn(lastFinishDate)
        createController().onStart()
        apiSubject.onError(RuntimeException())
        verify(onFinish).invoke()
    }

    @Test
    fun `Invoke finish on successful api result`() {
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController().onStart()
        apiSubject.onSuccess(listOf(createBuild(finishDate = Date(1502103410000))))
        verify(onFinish).invoke()
    }

    @Test
    fun `Subscribe on given scheduler`() {
        val subscribeOn = TestScheduler()
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController(subscribeOnScheduler = subscribeOn).onStart()
        apiSubject.onSuccess(emptyList())
        verify(onFinish, never()).invoke()
        subscribeOn.triggerActions()
        verify(onFinish).invoke()
    }

    @Test
    fun `Observe on given scheduler`() {
        val observeOn = TestScheduler()
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController(observeOnScheduler = observeOn).onStart()
        apiSubject.onSuccess(emptyList())
        verify(onFinish, never()).invoke()
        observeOn.triggerActions()
        verify(onFinish).invoke()
    }

    @Test
    fun `Clear disposable on stop`() {
        whenever(recapRepository.lastFinishDate).thenReturn(Date(1502103373000))
        createController().run {
            onStart()
            onStop()
        }
        apiSubject.onError(RuntimeException())
        verify(onFinish, never()).invoke()
    }

    private fun createController(projectsIds: List<String>? = null,
                                 subscribeOnScheduler: Scheduler = trampoline(),
                                 observeOnScheduler: Scheduler = trampoline()) =
            RecapController(loginRepository, recapRepository, projectsIds, api, notifier, onFinish,
                    SchedulersSupplier(subscribeOnScheduler, observeOnScheduler))
}