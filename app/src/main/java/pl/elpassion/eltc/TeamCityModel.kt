package pl.elpassion.eltc

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class TeamCityModel(private val api: TeamCityApi,
                    private val repository: Repository) {

    private val stateSubject = BehaviorSubject.createDefault<AppState>(InitialState)
    val state: Observable<AppState> = stateSubject

    private fun goTo(state: AppState) = stateSubject.onNext(state)

    private val refreshInterval = Observable.interval(3, TimeUnit.SECONDS)

    private val refreshDisposable = CompositeDisposable()

    fun perform(action: UserAction) {
        when (action) {
            is StartApp -> loadBuilds()
            is SubmitCredentials -> performSubmitCredentials(action)
            is RefreshList -> loadBuilds()
            is AutoRefresh -> performAutoRefresh(action.enable)
            is SelectProjects -> performSelectProjects()
        }
    }

    private fun loadBuilds() {
        val authData = repository.authData
        if (authData != null) {
            getBuildsAndProjects(authData.credentials)
        } else {
            goTo(LoginState)
        }
    }

    private fun performSubmitCredentials(action: SubmitCredentials) = with(action) {
        repository.authData = AuthData(address, credentials)
        getBuildsAndProjects(credentials)
    }

    private fun performSelectProjects() {
        state.firstElement().subscribe {
            (it as? MainState)?.let { goTo(SelectProjectsDialogState(it.projects)) }
        }
    }

    private fun performAutoRefresh(enable: Boolean) {
        refreshDisposable.clear()
        if (enable)
            refreshInterval
                    .subscribe { perform(RefreshList) }
                    .let { refreshDisposable.add(it) }
    }

    private fun getBuildsAndProjects(credentials: String) {
        val onNext: (Pair<List<Build>, List<Project>>) -> Unit = { (builds, projects) ->
            goTo(MainState(builds, projects))
        }
        val onError: (Throwable) -> Unit = { error ->
            if (error is TeamCityApiException) {
                goTo(error.toState())
            } else {
                stateSubject.onError(error)
            }
        }
        goTo(LoadingState)
        Single.zip<List<Build>, List<Project>, Pair<List<Build>, List<Project>>>(
                api.getBuilds(credentials),
                api.getProjects(credentials),
                BiFunction { builds, projects ->
                    builds to projects
                })
                .subscribe(onNext, onError)
    }

    private fun TeamCityApiException.toState() = when (this) {
        is UnknownHostException -> UnknownHostState
        is InvalidCredentialsException -> InvalidCredentialsState
        is NetworkTimeoutException -> NetworkProblemState
    }
}