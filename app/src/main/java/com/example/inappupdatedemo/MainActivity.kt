package com.example.inappupdatedemo

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

private const val APP_UPDATE_TYPE_SUPPORTED = AppUpdateType.FLEXIBLE
private const val REQUEST_UPDATE = 100
class MainActivity : AppCompatActivity() {
    private lateinit var updateListner: InstallStateUpdatedListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkForUpdates()
    }

    private fun checkForUpdates(){
        val appUpdateManager : AppUpdateManager
        if(BuildConfig.DEBUG){
            appUpdateManager = FakeAppUpdateManager(baseContext)
            appUpdateManager.setUpdateAvailable(2)
        } else {
            appUpdateManager = AppUpdateManagerFactory.create(baseContext)
        }
        val appUpdateInfo = appUpdateManager.appUpdateInfo
        appUpdateInfo.addOnSuccessListener {
            handleUpdate(appUpdateManager, appUpdateInfo)
        }
    }

    private fun handleUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>){
        if(APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.IMMEDIATE){
            handleImmediateUpdate(manager, info)
        }else if(APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.FLEXIBLE){
            handleFlexibleUpdate(manager, info)
        }
    }

    private fun handleImmediateUpdate(manager:AppUpdateManager, info: Task<AppUpdateInfo>){
        if((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
                info.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)){
            manager.startUpdateFlowForResult(info.result, AppUpdateType.IMMEDIATE, this, REQUEST_UPDATE)
        }
        if(BuildConfig.DEBUG){
            val fakeAppUpdate = manager as FakeAppUpdateManager
            if(fakeAppUpdate.isImmediateFlowVisible){
                fakeAppUpdate.userAcceptsUpdate()
                fakeAppUpdate.downloadStarts()
                fakeAppUpdate.downloadCompletes()
                launchRestartDialog(manager)
            }
        }
    }

    private fun handleFlexibleUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>){
        if((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) && info.result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)){
            btn_update.visibility = View.VISIBLE
            setUpdateAction(manager, info)
           manager.completeUpdate()
        }

    }

    private fun setUpdateAction(manager: AppUpdateManager, info: Task<AppUpdateInfo>){
        btn_update.setOnClickListener{
            updateListner = InstallStateUpdatedListener {
                btn_update.visibility = View.GONE
                tv_status.visibility = View.VISIBLE

                when (it.installStatus()){
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        tv_status.text = getString(R.string.info_failed)
                        btn_update.visibility = View.VISIBLE
                    }
                    InstallStatus.PENDING -> {
                        tv_status.text = getString(R.string.info_pending)
                    }
                    InstallStatus.CANCELED -> {
                        tv_status.text = getString(R.string.info_canceled)
                    }
                    InstallStatus.DOWNLOADING -> {
                        tv_status.text = getString(R.string.info_downloading)
                    }
                    InstallStatus.DOWNLOADED -> {
                        tv_status.text = getString(R.string.info_installing)
                        launchRestartDialog(manager)
                    }
                    InstallStatus.INSTALLING -> {
                        tv_status.text = getString(R.string.info_installing)
                    }
                    InstallStatus.INSTALLED -> {
                        tv_status.text = getString(R.string.info_installed)
                        manager.unregisterListener(updateListner)
                    }
                    else -> {
                        tv_status.text = getString(R.string.info_restart)
                    }
                }
            }
            manager.registerListener(updateListner)
            manager.startUpdateFlowForResult(info.result, AppUpdateType.FLEXIBLE, this, REQUEST_UPDATE)
            if(BuildConfig.DEBUG){
                val fakeAppUpdate = manager as FakeAppUpdateManager
                if(fakeAppUpdate.isConfirmationDialogVisible){
                    fakeAppUpdate.userAcceptsUpdate()
                    fakeAppUpdate.downloadStarts()
                    fakeAppUpdate.downloadCompletes()
                    fakeAppUpdate.completeUpdate()
                    fakeAppUpdate.installCompletes()
                }
            }
        }
    }

    private fun launchRestartDialog(manager : AppUpdateManager){
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_title))
            .setMessage(getString(R.string.update_message))
            .setPositiveButton(getString(R.string.action_restart)){
                _, _ -> manager.completeUpdate()
            }
            .create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        if(REQUEST_UPDATE == requestCode){
            when(resultCode){
                Activity.RESULT_OK -> {
                    if(APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.IMMEDIATE){
                        Toast.makeText(baseContext, R.string.toast_updated, Toast.LENGTH_SHORT).show()
                    } else{
                        Toast.makeText(baseContext, R.string.toast_started, Toast.LENGTH_SHORT).show()
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(baseContext, R.string.toast_cancelled, Toast.LENGTH_SHORT).show()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Toast.makeText(baseContext, R.string.toast_failed, Toast.LENGTH_SHORT).show()
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

