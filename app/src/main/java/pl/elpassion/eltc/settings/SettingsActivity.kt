package pl.elpassion.eltc.settings

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.settings_activity.*
import pl.elpassion.eltc.*

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(toolbar)
        showBackArrowInToolbar()
        initModel()
    }

    override fun showState(state: AppState?) = Unit

    override fun onBackPressed() {
        model.perform(ReturnToList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> model.perform(ReturnToList)
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }
}