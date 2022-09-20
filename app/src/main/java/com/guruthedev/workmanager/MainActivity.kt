package com.guruthedev.workmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    companion object{
        /**
        How to set up Input and output data in the Work Manager ?
        Working with a Work Manager you might need to pass some arguments to a task. To do that we need to create
        a data object  and save the data object to the request objects.
         */
        const val KEY_COUNT_VALUE = "key_count"
    }
    private val textViews: TextView
        get() = findViewById(R.id.textView1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.button)
        btn.setOnClickListener {
            setOneTimeWorkRequest()
            setPeriodWorkRequest()
        }
    }

    /**
     *OneTimeWorkRequest:-
    We can schedule this task to run at an appropriate time on the internet and the battery charger level of this device
    type of work request is called One Time Work Request.
     */
    private fun setOneTimeWorkRequest(){
        val workManager:WorkManager = WorkManager.getInstance(applicationContext)


        val data:Data = Data.Builder()
            .putInt(KEY_COUNT_VALUE,125)
            .build()

        /**
        Constraints:-
        Work manager uses constraints provided by us to decide when the work should run as.
         */

        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        /**
        Chaining Workers
        With the work manager we can sequential and parallel chain different tasks as .
         */

        val filteringRequest = OneTimeWorkRequest.Builder(FilteringWorker::class.java)
            .build()

        val compressingRequest = OneTimeWorkRequest.Builder(CompressingWorker::class.java)
            .build()

        val downloadingRequest = OneTimeWorkRequest.Builder(DownloadingWorker::class.java)
            .build()
        val parallelWork : MutableList<OneTimeWorkRequest> = mutableListOf<OneTimeWorkRequest>()
        parallelWork.add(downloadingRequest)
        parallelWork.add(filteringRequest)

        workManager
            .beginWith(parallelWork)
            .then(compressingRequest)
            .then(uploadRequest)
            .enqueue()

        workManager.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                textViews.text = it.state.name

                if(it.state.isFinished){
                    val data:Data = it.outputData

                    val message:String? = data.getString(UploadWorker.KEY_WORKER)

                    Toast.makeText(applicationContext, message,Toast.LENGTH_LONG).show()


                }
            })
    }

    /**
     * PeriodicWorkRequest:-
    If we have the background work which is repeated periodically they should use periodic work request
    for now Android jetpack periodic work request has minimum period length of 15 minutes.

     */

    private fun setPeriodWorkRequest(){

        val periodicWorkRequest = PeriodicWorkRequest.Builder(DownloadingWorker::class.java,16,TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
    }
}