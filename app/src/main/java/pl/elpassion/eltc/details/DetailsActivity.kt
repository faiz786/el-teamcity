package pl.elpassion.eltc.details

import android.os.Bundle
import kotlinx.android.synthetic.main.details_activity.*
import pl.elpassion.eltc.*
import java.text.SimpleDateFormat
import java.util.*

class DetailsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.details_activity)
        setSupportActionBar(toolbar)
        initModel()
    }

    override fun onBackPressed() {
        model.perform(ReturnToList)
    }

    override fun showState(state: AppState?) {
        when (state) {
            is BuildDetailsState -> showDetails(state.build)
            is LoadingState -> openBuildsScreen()
        }
    }

    private fun showDetails(build: Build) {
        toolbar.title = "#${build.number}"
        projectName.text = build.buildType.projectName
        buildStatusText.text = build.statusText
        buildTime.text = build.time
    }
}

private val Build.time: String
    get() = if (finishDate != null) {
        "Time: $totalTime"
    } else {
        "Started at: $startTime"
    }

private val Build.totalTime: String
    get() = "$startTime - $finishTime"

private val Build.startTime: String?
    get() = SimpleDateFormat("d MMM YY HH:mm:ss", Locale.US).format(startDate)

private val Build.finishTime: String?
    get() = if (startDate?.day == finishDate?.day) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(finishDate)
    } else {
        SimpleDateFormat("d MMM YY HH:mm:ss", Locale.US).format(finishDate)
    }