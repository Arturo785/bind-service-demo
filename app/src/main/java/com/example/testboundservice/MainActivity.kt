package com.example.testboundservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.testboundservice.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.testboundservice.Constants.ACTION_STOP_SERVICE
import com.example.testboundservice.Constants.LOG_SERVICE
import com.example.testboundservice.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    lateinit var mService: MyService
    private var mIsBound = false


    /**This is the object we can use to detect when the client (activity) has successfully bound to the service,
    and when it has disconnected from the service.*/
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binderParam: IBinder?) {
            Log.d(LOG_SERVICE, "onServiceConnected: ")
            val binder = binderParam as MyService.MyBinder
            mService = binder.getService()
            mIsBound = true

            getRandomNumberFromService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(LOG_SERVICE, "onServiceDisconnected")
            mIsBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.stop.setOnClickListener {
            stopServiceOur()
        }

        binding.toggleUpdates.setOnClickListener {
            startService()
        }
    }

    /**
     * This way we return to listening the service because we bind to it again
     * but when entering 1st time to the screen with service not active we will not start it
     * if we use startService() we will awake the service whether we like or not
     * */
    override fun onResume() {
        super.onResume()
        //startService()
        bindToOurService()
    }


    override fun onStop() {
        super.onStop()
        if (mIsBound) {
            Log.d(LOG_SERVICE, "unbindService called")
            unbindService(serviceConnection)
            mIsBound = false
        }
    }


    /**
     *  Notice that bindService() is called after the service has started.
     *  That's very important if you want the service to continue running even when the client has disconnected
     * */
    //https://www.notion.so/Binding-a-service-74c6e53b6d7b40f495e90b266eee2026
    private fun startService() {
        val serviceIntent = Intent(this, MyService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE // we attach our own action
        }
        startService(serviceIntent)
        bindToOurService()
    }

    private fun stopServiceOur() {
        val serviceIntent = Intent(this, MyService::class.java).also {
            it.action = ACTION_STOP_SERVICE // we attach our own action
        }

        startService(serviceIntent)
        if (mIsBound) {
            Log.d(LOG_SERVICE, "unbindService called")
            unbindService(serviceConnection)
            mIsBound = false
        }
    }

    private fun bindToOurService() {
        val serviceBindIntent = Intent(this, MyService::class.java)
        bindService(serviceBindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun getRandomNumberFromService() {
        lifecycleScope.launch {
            Log.d(
                LOG_SERVICE,
                "FLOW COLLECTION STARTED"
            )

            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mService.numberGenerated.collectLatest {
                        binding.textView.text = it.toString()
                    }
                }
            }
        }
    }


}