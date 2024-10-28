/**
 * Copyright (c) 2024 Natalia Molinero Mingorance
 * All rights reserved.
 */

package wamboo.example.videocompressor

import android.Manifest
import android.app.*
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaCodecList
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.work.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformationSession
import com.github.mikephil.charting.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.*
import wamboo.example.videocompressor.databinding.FragmentHomeBinding
import wamboo.example.videocompressor.workers.ForegroundWorker
import wamboo.example.videocompressor.workers.VideoCompressionWorker
import java.math.RoundingMode
import kotlin.math.round
import java.util.*
import com.google.android.ump.*
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateFailureListener
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateSuccessListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

private const val REQUEST_PICK_VIDEO = 1
private const val UPDATE_REQUEST_CODE = 1001

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var mAdView: AdView
    private lateinit var logoH : ImageView
    private var videoUrl: Uri? = null
    private var compressedFilePath = ""
    private var typesSpinner=arrayOf("")
    private var formatsSpinner=arrayOf("")
    private var formatsValues=arrayOf("")
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var mediaInformation : MediaInformationSession
    private var initialSize = ""
    private lateinit var videoHeight : String
    private lateinit var  videoWidth : String
    private var  videoResolution =""
    private var videoResolutionInit = ""
    private var  showSpeed =""
    private var  showCodec =""
    private var  videoCodec =""
    private var  compressSpeed =""
    private var  bitrate =""
    private var  fps =""
    private lateinit var videoView1: VideoView
    private lateinit var videoView2: VideoView
    private var audio = "-c:a copy"
    private lateinit var binding: FragmentHomeBinding
    private var selectedtype = "Ultrafast"
    private var selectedformat = "mp4"
    private var videoformat = "mp4"
    private lateinit var progressDialog: AlertDialog
    private var showViews = true
    private lateinit var consentInformation: ConsentInformation
    private lateinit var consentForm: ConsentForm
    var  init75 = 0.0
    var init40= 0.0
    var init50= 0.0
    var init70= 0.0
    var unidades75 = "MB"
    var unidades40 = "MB"
    var unidades70 = "MB"
    var unidades50 = "MB"
    //this receiver will trigger when the compression is completed
    private val videoCompressionCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.WORK_COMPLETED_ACTION) {
                progressDialog.dismiss()
                Log.d("Service", "Broadcast run")

                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success

                    val msg1 = getString(R.string.notification_message_success)
                    Toast.makeText(context, msg1, Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(requireActivity()).apply {
                        val msg2 = getString(R.string.scroll)

                        setMessage(msg2).setPositiveButton(
                            "OK"
                        ) { _, _ -> (requireActivity()) }
                    }.create().show()
                    showDataFromPref()


                } else {
                    val msg1 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, msg1, Toast.LENGTH_SHORT).show()
                }
                if (compressedFilePath != intent.getStringExtra(URI_PATH)){

                    compressedFilePath = intent.getStringExtra(URI_PATH).toString()
                }
                // after successful retrieval of the video and properly
                // setting up the retried video uri in
                binding.videoView2.setVideoURI(Uri.parse(compressedFilePath))



                // Configure VideoView and make the controls visible when it's ready
                binding.videoView2.visibility = View.VISIBLE

                // Listener to know when the video is ready to play
                binding.videoView2.setOnPreparedListener {

                    val mediaController2 = MediaController(requireActivity())

                    mediaController2.setAnchorView(binding.videoView2)

                    mediaController2.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                    mediaController2.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                    // Apply customized background
                    mediaController2.setBackgroundResource(R.drawable.rounded_media_controller)

                    binding.videoView2.setMediaController(mediaController2)
                    binding.videoView2.start()
                    mediaController2.show(0)
                    // Forze MediaController disappear after 3 seconds (3000 ms)

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        mediaController2.hide()  // Hide MediaController after 3 seconds
                    }, 3000)  // Timne in miliseconds
                }
                // Check if the video is corrupted
                checkVideoCorruption(requireContext(), Uri.parse(compressedFilePath))

                if (videoResolution == videoResolutionInit) {
                    binding.quality.text =""
                    binding.quality.visibility= View.VISIBLE
                    binding.qualityDescription.visibility= View.VISIBLE
                    binding.checkboxQuality.visibility= View.VISIBLE
                    binding.checkboxQuality.setOnCheckedChangeListener{ _, _ ->
                        val checked: Boolean = binding.checkboxQuality.isChecked
                        if (checked) {
                            /*val snack = Snackbar.make(binding.compressVideo,getString(R.string.waiting),Toast.LENGTH_SHORT)
                            snack.setAnchorView(binding.compressVideo)
                            snack.show()*/



                            calculateQuality()

                        }


                    }
                } else {
                    binding.quality.visibility= View.GONE
                    binding.qualityDescription.visibility= View.GONE
                    binding.checkboxQuality.visibility= View.GONE
                    binding.quality.text =""
                }

            }

        }

    }

    private val videoCompressionProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == Constants.WORK_PROGRESS_ACTION) {
                compressedFilePath = intent.getStringExtra(URI_PATH).toString()
                // Do something when the WorkManager completes its work
                // For example, update UI, show a notification, etc.
                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success
                    val percentage = intent.getStringExtra("percentage")
                    val msg = getString(R.string.waiting)
                    progressDialog.setMessage(msg + "$percentage")
                    if (progressDialog.isShowing.not()) {
                        progressDialog.show()
                    }
                } else {
                    val msg2 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, msg2, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun checkVideoCorruption(context: Context, filePath: Uri) {
        if (filePath != null) {

            val mediaInformationSession  = FFprobeKit.getMediaInformation(
                FFmpegKitConfig.getSafParameterForRead(
                    activity,
                    filePath
                )
            )

            videoWidth = mediaInformation.mediaInformation.streams[0].width.toString()
            videoHeight = mediaInformation.mediaInformation.streams[0].height.toString()
            // Check that the session was successful and get info from the video
            if (mediaInformationSession != null  ) {
                val mediaInformation = mediaInformationSession.mediaInformation
                val streams = mediaInformation?.streams

                if (streams != null) {
                    // Iterate the streams to find the one with "codec_type = video"
                    for (stream in streams) {

                        if (stream.getStringProperty("codec_type") == "video") {
                            // Get the coded_width
                            val codedWidth = stream.getStringProperty("coded_width")
                            if (codedWidth != null) {
                                Log.d("VideoInfo", "Coded Width: $codedWidth")

                                // Compare coded_width with the width selected for the compression
                                if (codedWidth == videoWidth || codedWidth == videoHeight) {
                                    Log.d("Comparison", "La resolución coincide con la seleccionada.")

                                } else {
                                    Log.d(
                                        "Comparison",
                                        "La resolución no coincide con la seleccionada."
                                    )
                                    val codecName = stream.getStringProperty("codec_name")
                                    val codedWidth = codedWidth?.toIntOrNull() ?: 0
                                    val videoWidth = videoWidth?.toIntOrNull() ?: 0
                                    val alertMessage = getString(
                                        R.string.video_resolution_mismatch,
                                        codedWidth,
                                        videoWidth
                                    )
                                    AlertDialog.Builder(requireActivity()).apply {
                                        setTitle(getString(R.string.corrupted_video))
                                        setMessage(alertMessage)
                                        setPositiveButton("OK") { _, _ -> }
                                        setCancelable(false)
                                    }.create().show()
                                    // If codec is H.265, suggest to change to H.264
                                    if (codecName == "hevc") {
                                        val alertMessage = getString(R.string.video_h265_warning)



                                        AlertDialog.Builder(requireActivity()).apply {
                                            setTitle(getString(R.string.corrupted_video))
                                            setMessage(alertMessage)
                                            setPositiveButton("OK") { _, _ -> }
                                            setCancelable(false)
                                        }.create().show()
                                    }
                                }
                            } else {
                                Log.e("VideoInfo", "Coded Width no encontrado.")
                            }
                            break // End the loop once the stream is found
                        }
                    }
                } else {
                    Log.e("MediaInformation", "No se encontraron streams en la información del video.")
                }
            } else {
                Log.e("FFprobeKit", "Error al obtener la información del video.")
            }
        }
    }


    private fun calculateQuality() {
        if (activity != null && videoUrl != null && compressedFilePath.isNotEmpty()) {
            binding.quality.visibility = View.GONE
            binding.qualityDescription.visibility = View.GONE
            binding.checkboxQuality.visibility = View.GONE
            binding.quality.text = ""

            val command2 = "-i ${
                FFmpegKitConfig.getSafParameterForRead(
                    activity,
                    videoUrl
                )
            } -i ${
                FFmpegKitConfig.getSafParameterForRead(
                    activity,
                    Uri.parse(compressedFilePath)
                )
            } -lavfi \"ssim;[0:v][1:v]psnr\" -f null -"

            Log.d(TAG, "Comando FFmpeg: $command2")

            Toast.makeText(
                context,
                Html.fromHtml("<font color='red' ><b>" + getString(R.string.quality_progress) + "</b></font>"),
                Toast.LENGTH_SHORT
            ).show()

            // We use AsyncTask to run the command in a background thread
            object : AsyncTask<Void, Void, Double?>() {
                override fun doInBackground(vararg params: Void?): Double? {
                    return try {
                        val hola = FFmpegKit.execute(command2)
                        val indexSsim = hola.logs.size
                        val ssimLine = hola.logs.get(indexSsim - 2)
                        val ssim = ssimLine.message.substringAfter("All:").substringBefore("(")
                        if (ssim.contains("0.")) {
                            (1 - ssim.toDouble()) * 100
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al ejecutar FFmpeg: ${e.message}", e)
                        null
                    }
                }

                override fun onPostExecute(result: Double?) {
                    if (result != null) {
                        val quality = result.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
                        binding.quality.text = "$quality%"
                        AlertDialog.Builder(requireActivity()).apply {
                            setMessage(getString(R.string.quality_completed) + " " + quality + "%")
                            setPositiveButton("OK") { _, _ -> }
                            setCancelable(false)
                        }.create().show()
                    } else {
                        binding.quality.text = getString(R.string.poor_quality)
                        AlertDialog.Builder(requireActivity()).apply {
                            setMessage(getString(R.string.poor_quality))
                            setPositiveButton("OK") { _, _ -> }
                            setCancelable(false)
                        }.create().show()
                    }

                    binding.quality.visibility = View.VISIBLE
                    binding.qualityDescription.visibility = View.VISIBLE
                    binding.checkboxQuality.visibility = View.VISIBLE
                }
            }.execute()
        }
    }

    private fun checkForInAppUpdate() {
        // Create an AppUpdateManager instance
        val appUpdateManager = AppUpdateManagerFactory.create(requireActivity())
        // Get info about the available update
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Log.d("InAppUpdate", "Update info received: ${appUpdateInfo.updateAvailability()}")
            // Check if there is an available update and if the type is allowed
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Log.d("InAppUpdate", "Update available and allowed")

                // Initiate the update process
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    requireActivity(),
                    UPDATE_REQUEST_CODE
                )
            } else {
                Log.d("InAppUpdate", "No update available or not allowed")
            }
        }.addOnFailureListener { exception ->
            Log.e("InAppUpdate", "Failed to check for updates", exception)

        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("InAppUpdate", "Update completed successfully")
                Toast.makeText(requireActivity(), getString(R.string.update_successful), Toast.LENGTH_LONG).show()
            } else {
                Log.e("InAppUpdate", "Update failed with resultCode: $resultCode")
                Toast.makeText(requireActivity(), getString(R.string.update_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showStats(
        initialSize2: String?,
        compressedSize: String?,
        conversionTime: String?,
        initialBattery: String?,
        remainingBattery: String?,
        co2: String?,
        showView: Boolean
    ) {
        initialSize = initialSize2!!
        //showing stats data in the textviews
        if (!showView){
            binding.videoView.visibility = View.GONE
            binding.videoView1.visibility = View.GONE
            binding.deleteVideo.visibility = View.GONE
            binding.videoView2.visibility = View.GONE
            binding.spinner.visibility = View.GONE
            binding.spinner5.visibility = View.GONE
            binding.spinner2.visibility = View.GONE
            binding.spinner3.visibility = View.GONE
            binding.spinner4.visibility = View.GONE
            binding.checkboxAudio.visibility = View.GONE
            binding.dataTV.visibility=View.GONE
            binding.dataTV2.visibility=View.GONE
            binding.dataTV3.visibility=View.GONE
        }
        when (videoUrl) {
            null -> {
                binding.videoView1.visibility = View.GONE
                binding.deleteVideo.visibility = View.GONE
                // Configure VideoView and make the controls visible when it's ready
                binding.videoView2.visibility = View.VISIBLE

                // Listener to know when the video is ready to play
                binding.videoView2.setOnPreparedListener {

                    val mediaController2 = MediaController(requireActivity())

                    mediaController2.setAnchorView(binding.videoView2)

                    mediaController2.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                    mediaController2.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                    // Apply customized background
                    mediaController2.setBackgroundResource(R.drawable.rounded_media_controller)

                    binding.videoView2.setMediaController(mediaController2)
                    binding.videoView2.start()
                    mediaController2.show(0)
                    // Forze MediaController disappear after 3 seconds (3000 ms)

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        mediaController2.hide()  // Hide MediaController after 3 seconds
                    }, 3000)  // Timne in miliseconds
                }

            }
            else -> {
                binding.deleteVideo.visibility = View.VISIBLE
                // Configure VideoView and make the controls visible when it's ready
                binding.videoView1.visibility = View.VISIBLE

                // Listener to know when the video is ready to play
                binding.videoView1.setOnPreparedListener {

                    val mediaController1 = MediaController(requireActivity())

                    mediaController1.setAnchorView(binding.videoView1)

                    mediaController1.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                    mediaController1.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                    // Apply customized background
                    mediaController1.setBackgroundResource(R.drawable.rounded_media_controller)

                    binding.videoView1.setMediaController(mediaController1)
                    binding.videoView1.start()
                    mediaController1.show(0)
                    // Forze MediaController disappear after 3 seconds (3000 ms)

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        mediaController1.hide()  // Hide MediaController after 3 seconds
                    }, 3000)  // Timne in miliseconds
                }
                // Configure VideoView and make the controls visible when it's ready
                binding.videoView2.visibility = View.VISIBLE

                // Listener to know when the video is ready to play
                binding.videoView2.setOnPreparedListener {

                    val mediaController2 = MediaController(requireActivity())

                    mediaController2.setAnchorView(binding.videoView2)

                    mediaController2.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                    mediaController2.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                    // Apply customized background
                    mediaController2.setBackgroundResource(R.drawable.rounded_media_controller)

                    binding.videoView2.setMediaController(mediaController2)
                    binding.videoView2.start()
                    mediaController2.show(0)
                    // Forze MediaController disappear after 3 seconds (3000 ms)

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        mediaController2.hide()  // Hide MediaController after 3 seconds
                    }, 3000)  // Timne in miliseconds
                }

            }
        }
        binding.pickVideo.visibility = View.VISIBLE
        binding.reset.visibility = View.VISIBLE
        binding.shareVideo.visibility = View.VISIBLE
        binding.statsContainer.visibility = View.VISIBLE
        binding.initialSizeTV.text = initialSize
        binding.compressedSizeTV.text = compressedSize
        binding.conversionTimeTV.text = conversionTime
        binding.initialBatteryTV.text = initialBattery
        binding.remainingBatteryTV.text = remainingBattery
        showViews = false
        val pollution= co2!!.toDouble()
        if (pollution > 0) {
            binding.co2TV.setTextColor(Color.parseColor("#FF0000"))
            binding.co2TV.text = co2+ "kgCO2"

        }else{
            binding.co2TV.setTextColor(Color.parseColor("#6F9F3A"))
            binding.co2TV.text = buildString {
                append(co2)
                append("kgCO2")
                append("\n")
                append(getString(R.string.congrats))
            }

        }
        if (compressedSize != null && initialSize != "") {
            val finalSize = compressedSize.substringBefore(" ")
            val finalSizeCheck = finalSize.replace(",", "").toDoubleOrNull()
            var finalS= 100.0
            if (finalSizeCheck != null) {
                if (finalSizeCheck >1000 ) {
                    finalS = finalSize.replace(".", "").replace(",", ".").toDouble()
                }else {finalS = finalSize.replace(",", ".").toDouble()}
            }
            var final = finalS
            if ((compressedSize.contains("k") && initialSize.contains("M") )||(compressedSize.contains("M") && initialSize.contains("G") ) ||(compressedSize.contains("B") && initialSize.contains("k") )){
                final=finalS/1000
            }
            val initSize = initialSize.substringBefore(" ")
            val init = initSize.replace(",",".")
            val sizeReduction = (100- final.times(100).div(init.toDouble()).toBigDecimal().setScale(2,
                RoundingMode.UP)?.toDouble()!!).toBigDecimal().setScale(2,
                RoundingMode.UP)
            if (sizeReduction<1.toBigDecimal()){
                binding.reduction.text = buildString {
                    append(sizeReduction.toString())
                    append("%")
                    append(" ")
                    append(getString(R.string.noreduction))
                }
            } else {
                binding.reduction.text = buildString {
                    append(sizeReduction.toString())
                    append("%")
                }
            }

        }


    }

    // This is the first method automatically called when the activity starts .
    // Here we initialize all the data which we are going to use and attach the
    // xml layout file with the kotlin code to show the layout and make changes in it when needed
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        savedInstanceState?.let {
            videoUrl = it.getParcelable("videoUrl")
        }
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        pref = requireActivity().getSharedPreferences(
            requireActivity().packageName, Context.MODE_PRIVATE
        )
        editor = pref.edit()

        // Progress receiver register
        requireContext().registerReceiver(
            videoCompressionProgressReceiver,
            IntentFilter(Constants.WORK_PROGRESS_ACTION),
            Context.RECEIVER_EXPORTED
        )

// Finalization receiver register
        requireContext().registerReceiver(
            videoCompressionCompletedReceiver,
            IntentFilter(Constants.WORK_COMPLETED_ACTION),
            Context.RECEIVER_EXPORTED
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    //clearPref()
                    requireActivity().finish()
                }

            })
        logoH =binding.root.findViewById(R.id.bottomImage)
        logoH.setOnClickListener(){
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse("https://www.instagram.com/harmonyvalley_official/")
            startActivity(intent)
        }
        return binding.root
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save state
        outState.putParcelable("videoUrl", videoUrl)
    }
    private fun showDataFromPref() {

        if (pref.getString(RETURN_CODE, "")?.isNotEmpty() == true) {

            showStats(
                pref.getString(INITIAL_SIZE, ""),
                pref.getString(COMPRESS_SZE, ""),
                pref.getString(CONVERSION_TIME, ""),
                pref.getString(INITIAL_BATTERY, ""),
                pref.getString(REMAINING_BATTERY, ""),
                pref.getString(CO2, ""),
                showViews
            )

            //clearPref()

        } else {
            showViews = false
        }
    }

    private fun clearPref() {
        editor.clear().commit()
    }
    private fun loadForm() {
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
            requireActivity(),
            UserMessagingPlatform.OnConsentFormLoadSuccessListener { form ->
                consentForm = form
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                        requireActivity(),
                        ConsentForm.OnConsentFormDismissedListener { dismissal ->
                            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                                // App can start requesting ads.
                            }

                            // Handle dismissal by reloading form.
                            loadForm()
                        }
                    )
                }
            },
            UserMessagingPlatform.OnConsentFormLoadFailureListener {
                // Handle the error.
            }
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (view != null) {
            super.onViewCreated(view, savedInstanceState)
            checkForInAppUpdate()
            // Set tag for under age of consent. false means users are not under age.
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            consentInformation = UserMessagingPlatform.getConsentInformation(requireContext())
            consentInformation.requestConsentInfoUpdate(
                requireActivity(),
                params,
                OnConsentInfoUpdateSuccessListener {
                    // The consent information state was updated.
                    // You are now ready to check if a form is available.
                    if (consentInformation.isConsentFormAvailable) {
                        // Make sure loadForm() is called in the main thread
                        requireActivity().runOnUiThread {
                            loadForm()
                        }
                    }
                },
                OnConsentInfoUpdateFailureListener {
                    // Handle the error.
                })
        }
        loadAd()
        //checkNotificationPermission()
        checkCameraPermission()
        initUI()
        videoView1 = binding.root.findViewById(R.id.videoView1)
        videoView2 = binding.root.findViewById(R.id.videoView2)
        showLoader()
    }
    private fun checkNotificationPermission() {

        val notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()

        if (!areNotificationsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create a channel for your notifications
                val channel = NotificationChannel(
                    "channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager =
                    requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                // Open the notification configuration of the app
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                }
                Log.d("IntentDebug", "Intent: ${intent.action} ${intent.data}")

                // Verify is there is an activity available to manage the Intent
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.error_config_notif),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.no_app_notif),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                // Open the app details for versions older than Android 0
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                }


                // Verify is there is an activity available to manage the Intent
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.error_settings),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string. no_app_settings),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }
    private fun checkCameraPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i("Permission: ", "Granted")
                } else {
                    Log.i("Permission: ", "Denied")
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.


                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.


                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA,
                )

            }
        }


    }

    override fun onResume() {
        super.onResume()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        Log.d("service", "OnResume")
        showDataFromPref()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().unregisterReceiver(videoCompressionCompletedReceiver)
        requireActivity().unregisterReceiver(videoCompressionProgressReceiver)
    }

    private fun showLoader() {

        val builder = AlertDialog.Builder(
            requireActivity()
        )
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.progress_dialog_layout, null)
        builder.setView(dialogView)
            .setCancelable(true)
            .setNegativeButton(getString(R.string.cancel_compression)) { dialog, which ->
                progressDialog.dismiss()
                deleteVideo()
                restartApp(requireContext())
            }

        progressDialog = builder.create()


    }

    fun deleteVideo() {
        val compressedVideoFilePath = getRealPathFromURI(requireContext(), Uri.parse(compressedFilePath))

        if (compressedVideoFilePath != null) {
            val compressedVideoFile = File(compressedVideoFilePath)

            if (compressedVideoFile.exists()) {
                val deleted = compressedVideoFile.delete()
                if (deleted) {
                    Log.d("DeleteVideo", "El archivo de video comprimido ha sido eliminado.")
                } else {
                    Log.e("DeleteVideo", "Error al eliminar el archivo de video.")
                }
            } else {
                Log.d("DeleteVideo", "El archivo de video comprimido no existe.")
            }
        } else {
            Log.e("DeleteVideo", "No se pudo obtener la ruta real del archivo desde la URI.")
        }
    }

    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    result = it.getString(columnIndex)
                }
            }
        } else if (uri.scheme == "file") {
            result = uri.path
        }
        return result
    }

    fun restartApp(context: Context) {
        // Create an intent to restart the app


        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Make sure the intent is not null
        if (intent != null) {

            context.startActivity(intent)
        } else {
            // Handle if the inten is null (very unlikely)
            Log.e("RestartApp", "Intent was null, unable to restart app.")
        }

        // End all the previous activities and exit the app
        exitProcess(0)
    }

    private fun loadAd() {
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView = binding.root.findViewById(R.id.adView)
        mAdView.loadAd(adRequest)

    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(requireActivity().packageName)) {

            AlertDialog.Builder(requireActivity()).apply {
                val msg2 = getString(R.string.background2)
                setMessage(msg2).setPositiveButton(
                    getString(R.string.ok)
                ) { _, _ ->  }
            }.create().show()

            return true
        } else {
            showAlertDialog()
            return false

        }

    }

    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity()).apply {
            val msg1 = getString(R.string.notification_background_title)
            val msg2 = getString(R.string.notification_background_body)
            setTitle(msg1)
            setMessage(msg2).setPositiveButton(
                getString(R.string.ok)
            ) { _, _ -> openBatteryUsagePage(requireActivity()) }
        }.create().show()
    }

    fun openBatteryUsagePage(ctx: Context) {
        val powerUsageIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromParts("package", ctx.packageName, null)
        powerUsageIntent.data = uri
        try {
            ctx.startActivity(powerUsageIntent)
        } catch (e: Exception) {
            Toast.makeText(
                ctx,
                getString(R.string.manual_battery_config),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Here we are initialising everything and setting click listeners on the code . Like what will happen
    // When the user tap on pick video button and other buttons
    private fun initUI() = with(binding) {
        reset.setOnClickListener {
            resetViews()
        }


        spinner.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        typesSpinner = arrayOf(getString(R.string.select_compression),getString(R.string.ultrafast),getString(R.string.good),getString(R.string.best),getString(R.string.half),getString(R.string.custom_h),getString(R.string.custom_l))
        val arrayAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_row, typesSpinner) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                // Change color to the "Reduce by half" option
                if (getItem(position) == getString(R.string.half)) {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else {
                    // Default color for the other options
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                // Apply bold to the "select_compression" option
                if (getItem(position) == getString(R.string.select_compression)) {
                    textView.setTypeface(null, Typeface.BOLD)
                } else {
                    textView.setTypeface(null, Typeface.NORMAL)
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                // Change color to the "Reduce by half" option
                if (getItem(position) == getString(R.string.half)) {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else {
                    // Default color for the other options
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                // Apply bold to the "select_compression" option
                if (getItem(position) == getString(R.string.select_compression)) {
                    textView.setTypeface(null, Typeface.BOLD)
                } else {
                    textView.setTypeface(null, Typeface.NORMAL)
                }
                return view
            }
        }

        spinner.adapter = arrayAdapter

        formatsSpinner =
            arrayOf(getString(R.string.select_format), "mp4", "avi", "mov", "mkv", "3gp")
        formatsValues = arrayOf("mp4", "mp4", "avi", "mov", "mkv", "3gp")
        val arrayAdapter2 = ArrayAdapter(requireContext(), R.layout.spinner_list, formatsSpinner)
        spinner5.adapter = arrayAdapter2
        pickVideo.setOnClickListener {


            // if (isBatteryOptimizationDisabled()) {
            resetViews()
            shareVideo.visibility = View.GONE
            deleteVideo.visibility = View.GONE
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
                    val intent2 = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    val chooser = Intent.createChooser(intent, "Some text here")
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent2))
                    resultLauncher.launch(chooser)                    }
                else -> {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
                    resultLauncher.launch(intent)

                }
            }



            //  }

        }

        rdOne.setOnClickListener{
            checkNotificationPermission()
            isBatteryOptimizationDisabled()
        }
        when (videoUrl) {
            null -> {
                binding.videoView1.visibility = View.GONE

            }
        }
        // Setting media controller to the video . So the user can pause and play the video . They will appear when user tap on video
        videoView.setMediaController(MediaController(requireActivity()))
        videoView1.setMediaController(MediaController(requireActivity()))
        videoView2.setMediaController(MediaController(requireActivity()))

        checkboxAudio.setOnCheckedChangeListener{ checkboxAudio, _ ->
            val checked: Boolean = checkboxAudio.isChecked
            if (checked) {
                audio = "-an"
            } else {
                audio = "-c:a copy"
            }
        }

        spinner5.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null){
                    selectedformat = formatsValues[position]
                    videoformat = formatsSpinner[position]
                    if (videoformat == "3gp" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_3gp),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (videoformat == "avi" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_avi),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    when (position) {
                        0 -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.no_selected_format),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.selected_format) + " " + videoformat,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        when (videoUrl) {
            null -> {
                binding.videoView.visibility = View.GONE
                Toast.makeText(context, Html.fromHtml("<font color='red' ><b>" +getString(R.string.select_video)+ "</b></font>"), Toast.LENGTH_SHORT).show()

            }

        }


        /*  with(spinner)
          {setSelection(0, false)}*/
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null){
                    selectedtype =typesSpinner[position]
                    val spinner2 =addSpinnerSpeed()
                    val spinner4 =addSpinnerCodec()
                    val spinner3 =addSpinnerResolution()
                    when (selectedtype) {
                        getString(R.string.good) ->{

                            hideSpinner(spinner2)
                            hideSpinner(spinner3)
                            hideSpinner(spinner4)
                            binding.dataTV.visibility=View.VISIBLE
                            binding.dataTV2.visibility=View.VISIBLE
                            binding.dataTV3.visibility=View.VISIBLE
                            binding.dataTV.text=getString(R.string.estimated_size)
                            binding.dataTV2.text=Html.fromHtml("<b>"+init40.toBigDecimal().setScale(2,RoundingMode.UP).toDouble()+" $unidades40"+"</b>")
                            binding.dataTV3.text="40% "+ getString(R.string.compression)
                        }
                        getString(R.string.best) ->{

                            hideSpinner(spinner2)
                            hideSpinner(spinner3)
                            hideSpinner(spinner4)
                            binding.dataTV.visibility=View.VISIBLE
                            binding.dataTV2.visibility=View.VISIBLE
                            binding.dataTV3.visibility=View.VISIBLE
                            binding.dataTV.text=getString(R.string.estimated_size)
                            binding.dataTV2.text=Html.fromHtml("<b>"+init70.toBigDecimal().setScale(2,RoundingMode.UP).toDouble()+" $unidades70"+"</b>")
                            binding.dataTV3.text="70% "+ getString(R.string.compression)
                        }
                        getString(R.string.ultrafast) ->{

                            hideSpinner(spinner2)
                            hideSpinner(spinner3)
                            hideSpinner(spinner4)
                            binding.dataTV.visibility=View.VISIBLE
                            binding.dataTV2.visibility=View.VISIBLE
                            binding.dataTV3.visibility=View.VISIBLE
                            binding.dataTV.text=getString(R.string.estimated_size)
                            binding.dataTV2.text=Html.fromHtml("<b>"  +init75.toBigDecimal().setScale(2,RoundingMode.UP).toDouble() +" $unidades75"+"</b>")
                            binding.dataTV3.text="50% "+ getString(R.string.compression)
                        }
                        getString(R.string.half) ->{
                            val streams = mediaInformation.mediaInformation.streams
                            var bitrateaux: Long? = null
                            // Select video stream (normally identified as "video")
                            for (stream in streams) {
                                if (stream.getStringProperty("codec_type") == "video") {
                                    // Get the bitrate from the video stream
                                    bitrateaux = stream.getStringProperty("bit_rate")?.toLongOrNull()

                                }
                            }

                            bitrate=(bitrateaux?.div(2)).toString()

                            hideSpinner(spinner2)
                            hideSpinner(spinner3)
                            hideSpinner(spinner4)
                            binding.dataTV.visibility=View.VISIBLE
                            binding.dataTV2.visibility=View.VISIBLE
                            binding.dataTV3.visibility=View.VISIBLE
                            binding.dataTV.text=getString(R.string.estimated_size)
                            binding.dataTV2.text=Html.fromHtml("<b>"  +init40.toBigDecimal().setScale(2,RoundingMode.UP).toDouble() +" $unidades40"+"</b>")
                            binding.dataTV3.text="40% "+ getString(R.string.compression)
                        }

                        getString(R.string.custom_h) ->{
                            dataTV.isVisible=false
                            dataTV2.isVisible=false
                            dataTV3.isVisible=false

                        }
                        getString(R.string.custom_l) ->{
                            dataTV.isVisible=false
                            dataTV2.isVisible=false
                            dataTV3.isVisible=false


                        }
                        else ->{

                            hideSpinner(spinner2)
                            hideSpinner(spinner3)
                            hideSpinner(spinner4)
                            binding.dataTV.visibility=View.VISIBLE
                            binding.dataTV2.visibility=View.VISIBLE
                            binding.dataTV3.visibility=View.VISIBLE
                            binding.dataTV.text=getString(R.string.estimated_size)
                            binding.dataTV2.text=Html.fromHtml("<b>"  +init75.toBigDecimal().setScale(2,RoundingMode.UP).toDouble()+" $unidades75"+"</b>")
                            binding.dataTV3.text="75% "+ getString(R.string.compression)
                        }
                    }


                }}
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout


        //  binding.spinner.visibility=View.VISIBLE



        compressVideo.setOnClickListener {

            //clearPref()
            statsContainer.visibility = View.GONE
            shareVideo.visibility = View.GONE
            deleteVideo.visibility = View.GONE
            binding.checkboxQuality.isChecked = false
            if (videoUrl != null) {

                compressVideo.isVisible = false
                val value =
                    fileSize(videoUrl!!.length(requireActivity().contentResolver))
                editor.putString(INITIAL_SIZE, value)
                editor.commit()

                // When the compress video button is clicked we check if video is already playing then we pause it
                if (videoView.isPlaying) {
                    videoView.pause()
                }
                if (videoView1.isPlaying) {
                    videoView1.pause()
                }
                if (videoView2.isPlaying) {
                    videoView2.pause()
                }
                // Set up the input data for the worker

                val data2 =
                    Data.Builder().putString(ForegroundWorker.VideoURI, videoUrl?.toString())
                        .putString(ForegroundWorker.SELECTION_TYPE, selectedtype)
                        .putString(ForegroundWorker.SELECTION_FORMAT, selectedformat)
                        .putString(ForegroundWorker.VIDEO_RESOLUTION, videoResolution)
                        .putString(ForegroundWorker.COMPRESS_SPEED, compressSpeed)
                        .putString(ForegroundWorker.VIDEO_CODEC, videoCodec)
                        .putString(ForegroundWorker.VIDEO_AUDIO, audio)
                        .putString(ForegroundWorker.BITRATE, bitrate)
                        .putString(ForegroundWorker.FPS, fps).build()

                // Create the work request
                val myWorkRequest =
                    OneTimeWorkRequestBuilder<VideoCompressionWorker>().setInputData(data2).build()

                //initiating WorkManager to start compressing the video
                WorkManager.getInstance(requireContext()).enqueue(myWorkRequest)

            } else {

                // If picked video is null or video is not picked
                binding.videoView.visibility = View.GONE
                binding.videoView1.visibility = View.GONE
                binding.videoView2.visibility = View.GONE
                Toast.makeText(context, Html.fromHtml("<font color='red' ><b>" +getString(R.string.select_video)+ "</b></font>"), Toast.LENGTH_SHORT).show()
            }

        }


        binding.shareVideo.setOnClickListener {
            ShareCompat.IntentBuilder(requireActivity()).setStream(Uri.parse(compressedFilePath))
                .setType("video/" + selectedformat).setChooserTitle(getString(R.string.share_compressed_video)).startChooser()
            binding.videoView.visibility = View.GONE

        }

        binding.deleteVideo.setOnClickListener {
            deleteOriginalVideoFromGallery(videoUrl)


        }
    }
    private fun deleteOriginalVideoFromGallery(videoUri: Uri?) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_PICK_VIDEO)
        videoUri?.let { videoUri2 ->
            val deleteIntent = Intent(Intent.ACTION_VIEW)
            deleteIntent.setDataAndType(videoUri2, "video/*")
            deleteIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            deleteIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            startActivity(deleteIntent)
        }


        videoUrl = intent.data



    }
    private fun resetViews() {
        with(binding) {

            clearPref()
            pickVideo.isVisible = true
            spinner5.isVisible=false
            spinner.isVisible=false
            spinner2.isVisible=false
            spinner3.isVisible=false
            spinner4.isVisible=false
            statsContainer.isVisible = false
            videoView.isVisible = false
            videoView1.isVisible = false
            videoView2.isVisible = false
            deleteVideo.isVisible=false
            checkboxAudio.isVisible=false
            shareVideo.isVisible=false
            compressVideo.isVisible = false
            reset.isVisible = false
            dataTV.isVisible=false
            dataTV2.isVisible=false
            dataTV3.isVisible=false
            rdOne.isChecked=false
            binding.quality.visibility= View.GONE
            binding.qualityDescription.visibility= View.GONE
            binding.checkboxQuality.visibility= View.GONE
            binding.quality.text =""
            spinner.setSelection(0)
            spinner5.setSelection(0)
            /*spinner2.setSelection(0)
            spinner3.setSelection(0)
            spinner4.setSelection(0)*/
        }
    }
    private fun addSpinnerResolution():Spinner {
        val streams = mediaInformation.mediaInformation.streams
        var bitrateaux: Long? = null
        var fpsaux: String? = null
        var videoRotation: Int? = null
        var videoHeight_aux = ""
        var videoWidth_aux = ""
        var side : Long? = null
        // Select video stream (normally identified as "video")
        for (stream in streams) {
            if (stream.getStringProperty("codec_type") == "video") {
                // Get the bitrate from the video stream
                bitrateaux = stream.getStringProperty("bit_rate")?.toLongOrNull()
                // Get the fps from the video stream
                fpsaux = stream.getStringProperty("avg_frame_rate")
                fps=fpsaux
                //Get the video rotation (if present)
                side = stream.getStringProperty("side_data_list")?.toLongOrNull()
            }
        }

        if (bitrateaux != null) {
            when {
                // If the bitrate is between 200 and 400 kbps
                bitrateaux in 200_000..400_000 -> {
                    videoWidth_aux = 426.toString()
                    videoHeight_aux = 240.toString()
                }
                // If the bitrate is between 400 and 800 kbps
                bitrateaux in 400_000..800_000 -> {
                    videoWidth_aux = 640.toString()
                    videoHeight_aux = 360.toString()
                }
                // If the bitrate is between 800 and 1500 kbps
                bitrateaux in 800_000..1_500_000 -> {
                    videoWidth_aux = 1280.toString()
                    videoHeight_aux = 720.toString()
                }
                // If the bitrate is between 4000 and 10000 kbps
                bitrateaux in 4_000_000..10_000_000 -> {
                    videoWidth_aux = 1920.toString()
                    videoHeight_aux = 1080.toString()
                }
                else -> {
                    // If bitrate is outside the specified ranges
                    println("El bitrate no está dentro de los rangos especificados")
                    videoWidth_aux = videoWidth
                    videoHeight_aux = videoHeight
                }
            }
        } else {
            println("El bitrate es null")
        }
        if (side != null) {
            videoRotation = side.toString().substringAfter("rotation\":").substringBefore('}').toIntOrNull()
        }
        // Adjust rotation
        if (videoRotation != null ) {
            // If rotation is 90 or 270 degress, swap width and height
            if (videoRotation == 90 || videoRotation == 270) {
                val temp = videoWidth_aux
                videoWidth_aux = videoHeight_aux
                videoHeight_aux = temp
            }
        }

        bitrate = bitrateaux.toString()

// if width and height are smaller than original, update the values
        if ((videoWidth_aux.toInt() < videoWidth.toInt() && videoHeight_aux.toInt() < videoHeight.toInt()) || (videoWidth_aux.toInt() < videoHeight.toInt() && videoHeight_aux.toInt() < videoWidth.toInt())) {

            if (videoWidth <videoHeight && videoWidth_aux>videoHeight_aux) {
                videoWidth = videoHeight_aux
                videoHeight = videoWidth_aux
            } else {
                videoWidth = videoWidth_aux
                videoHeight = videoHeight_aux
            }
        }
        // Mapa que contiene la combinación de resoluciones y los bitrates máximos permitidos
        val bitrateLimits = mapOf(
            Pair("416", "234") to Pair(145000,24),
            Pair("640", "360") to Pair(365000,24),
            Pair("768", "432") to Pair(1100000,24),
            Pair("960", "540") to Pair(2000000,fpsaux),
            Pair("1280", "720") to Pair(4500000,fpsaux),
            Pair("1920", "1080") to Pair(7800000, fpsaux)
        )


        fun findClosestResolution(width: Int, height: Int): Pair<String, String>? {
            return bitrateLimits.keys.minByOrNull { (mapWidth, mapHeight) ->
                // Calculamos la diferencia en área entre la resolución del video y las del mapa
                val mapWidthInt = mapWidth.toIntOrNull() ?: 0
                val mapHeightInt = mapHeight.toIntOrNull() ?: 0
                Math.abs(mapWidthInt * mapHeightInt - width * height)
            }
        }
        // Función para aplicar el límite de bitrate y retornar tanto el bitrate como el fps
        fun applyBitrateLimit(width: String, height: String, currentBitrate: String): String {
            val resolution = Pair(width, height)

            // Check if there is an FPS limit for the specified resolution
            bitrateLimits[resolution]?.let { (_, fpsValue) ->
                return fpsValue.toString()
            }

            // Find closest resolution if exact resolution not found
            val closestResolution = findClosestResolution(width.toInt(), height.toInt())
            closestResolution?.let { (closestWidth, closestHeight) ->
                val (_, fpsValue) = bitrateLimits[Pair(closestWidth.toString(), closestHeight.toString())] ?: return "30"
                return fpsValue.toString()
            }

            // Default to 30 FPS if no match found
            return "30"
        }

        // Función para verificar si el videoWidth o videoHeight coincide con la primera coordenada de algún par
        fun checkWidthOrHeightMatch(width: String, height: String): String {
            // Loop through the map to check if any dimension matches the first coordinate of the pair
            for ((resolution, _) in bitrateLimits) {
                if (resolution.first == width) {
                    // Apply bitrate limit and get only FPS
                    val fps = applyBitrateLimit(width, height, bitrate)
                    return fps.toString()
                } else if (resolution.first == height) {
                    val fps = applyBitrateLimit(height, width, bitrate) // For rotated dimensions
                    return fps.toString()
                }
            }

            // Default to fpsaux if no match found
            return fpsaux.toString()
        }


// Ejemplo de uso
        val fps = checkWidthOrHeightMatch(videoWidth, videoHeight)

        if (fps != fpsaux) {
            println("FPS ha cambiado.")
        } else {
            println("Se mantiene el FPS original")
        }




        binding.spinner3.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var resolutionSpinner =arrayOf("")
        var resolutionValues =arrayOf("")

        when (videoUrl) {
            null -> {




            }
            else -> {

                resolutionSpinner = arrayOf(
                    getString(R.string.select_resolution),
                    videoWidth + "x" + videoHeight + "(Original)",
                    "${(round((videoWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.7)/2)*2).toInt()}" + " (70%)",
                    "${(round((videoWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.5)/2)*2).toInt()}" + " (50%)",
                    "${(round((videoWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.25)/2)*2).toInt()}" + " (25%)",
                    "${(round((videoWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.05)/2)*2).toInt()}" + " (5%)"
                )
                resolutionValues = arrayOf(
                    videoWidth + "x" + videoHeight,
                    videoWidth + "x" + videoHeight,
                    "${(round((videoWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.7)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.5)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.25)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.05)/2)*2).toInt()}"
                )

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, resolutionSpinner)
        binding.spinner3.adapter = arrayAdapter
        /*with(binding.spinner3)
        {setSelection(0, false)}*/
        binding.spinner3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null) {
                    videoResolution = resolutionValues[position]

                    when (position) {
                        0 -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.no_selected_resolution),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.selected_resolution) + " " + resolutionSpinner[position],
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout

        binding.spinner3.visibility=View.VISIBLE
        return binding.spinner3

    }
    private fun addSpinnerSpeed():Spinner {


        binding.spinner2.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var speedsSpinner = arrayOf("")
        var speedValues = arrayOf("")
        when (videoUrl) {
            null -> {
                binding.videoView.visibility = View.GONE
                binding.videoView1.visibility = View.GONE
                Toast.makeText(context, Html.fromHtml("<font color='red' ><b>" +getString(R.string.select_video)+ "</b></font>"), Toast.LENGTH_SHORT).show()




            }
            else -> {
                compressSpeed ="ultrafast"
                speedsSpinner = arrayOf(getString(R.string.select_speed),getString(R.string.speed1),getString(R.string.speed2),getString(R.string.speed3),getString(R.string.speed4),getString(R.string.speed5),getString(R.string.speed6),getString(R.string.speed7),getString(R.string.speed8),getString(R.string.speed9))
                speedValues = arrayOf("ultrafast","ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow")

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, speedsSpinner)
        binding.spinner2.adapter = arrayAdapter
        /*with(spinner2)
        {setSelection(0, false)}*/
        binding.spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null) {
                    compressSpeed = speedValues[position]
                    showSpeed = speedsSpinner[position]
                    when (position) {
                        0 -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.no_selected_speed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.selected_speed) + " " + showSpeed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout

        binding.spinner2.visibility=View.VISIBLE
        return binding.spinner2

    }

    private fun addSpinnerCodec():Spinner {


        binding.spinner4.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var codecSpinner = arrayOf("")
        var codecValues = arrayOf("")
        when (videoUrl) {
            null -> {




            }
            else -> {
                videoCodec="libx264"
                codecSpinner = arrayOf(getString(R.string.select_codec),"H.264","H.265")
                codecValues = arrayOf("libx264","libx264","libx265")

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, codecSpinner)
        binding.spinner4.adapter = arrayAdapter
        /*with(binding.spinner4)
        {setSelection(0, false)}*/
        binding.spinner4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null) {
                    videoCodec = codecValues[position]
                    showCodec = codecSpinner[position]
                    if (videoformat == "3gp" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_3gp),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (videoformat == "avi" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_avi),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    when (position) {
                        0 -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.no_selected_codec),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.selected_codec) + " " + showCodec,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (! isHEVCCodecSupported()) {
                                // The device supports H.265
                                AlertDialog.Builder(requireActivity()).apply {
                                    setMessage(getString(R.string.no_hevc))
                                    setPositiveButton("OK") { _, _ -> }
                                    setCancelable(false)
                                }.create().show()
                            }
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout
        binding.spinner4.visibility=View.VISIBLE
        return binding.spinner4

    }
    fun isHEVCCodecSupported(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecs = codecList.codecInfos

        for (codecInfo in codecs) {
            // Check if the codec is decoder (not encoder)
            if (!codecInfo.isEncoder) {
                val types = codecInfo.supportedTypes
                for (type in types) {
                    // Check if the supported type is H.265 (HEVC)
                    if (type.equals("video/hevc", ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    private fun visibleViews() {

        with(binding) {
            pickVideo.isVisible = true
            videoView.isVisible = true
            videoView1.visibility = View.GONE
            videoView2.visibility = View.GONE
            spinner.isVisible=true
            spinner5.visibility = View.GONE
            checkboxAudio.isVisible=true
            compressVideo.isVisible = true
            showViews=true


        }
    }
    private fun hideSpinner(spinner: Spinner) {
        spinner.visibility= View.GONE


    }
    private fun isFrameRateAbove60(frameRate: String): Boolean {
        // Split the frame rate string into numerator and denominator
        val parts = frameRate.split("/")

        // Check if the frame rate string is in the expected format
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid frame rate format")
        }

        // Parse numerator and denominator
        val numerator = parts[0].toIntOrNull() ?: return false
        val denominator = parts[1].toIntOrNull() ?: return false

        // Calculate the frame rate as a floating-point number
        val frameRateValue = numerator.toDouble() / denominator.toDouble()

        // Check if the frame rate is greater than 60
        return frameRateValue > 60
    }

    /* This code is using the registerForActivityResult method to launch an activity for a result,
specifically to select a video file. If the result code is Activity.RESULT_OK, it means a video has been successfully selected.
The selected video's Uri is extracted from the Intent returned from the launched activity.
The code then sets the Uri to the VideoView and starts playing the video.
If there is an error in the process, an error message is displayed to the user via a Toast. */
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                visibleViews()
                val data: Intent? = result.data
                val videoUri: Uri? = result.data?.data
                videoUri?.let {
                    videoView1.setVideoURI(it)
                    //binding.videoView1.visibility = View.VISIBLE

                    // Listener to know when the video is ready to play
                    binding.videoView1.setOnPreparedListener {

                        val mediaController1 = MediaController(requireActivity())

                        mediaController1.setAnchorView(binding.videoView1)

                        mediaController1.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                        mediaController1.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                        // Apply customized background
                        mediaController1.setBackgroundResource(R.drawable.rounded_media_controller)

                        binding.videoView1.setMediaController(mediaController1)
                        binding.videoView1.start()
                        mediaController1.show(0)
                        // Forze MediaController disappear after 3 seconds (3000 ms)

                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            mediaController1.hide()  // Hide MediaController after 3 seconds
                        }, 3000)  // Timne in miliseconds
                    }
                    binding.spinner.isVisible = true
                    binding.spinner5.isVisible = true

                    binding.checkboxAudio.isVisible=true
                    videoUrl=it

                    //binding.compressVideo.isVisible = true
                }
                if (data != null) {
                    // get the video Uri
                    val uri: Uri? = data.data
                    try {
                        // get the file from the Uri using getFileFromUri() method present
                        // in FileUils.java
                        videoUrl = uri
                        if (videoUrl != null) {

                            mediaInformation = FFprobeKit.getMediaInformation(
                                FFmpegKitConfig.getSafParameterForRead(
                                    activity,
                                    videoUrl
                                )
                            )
                            var rate = mediaInformation.mediaInformation.streams[0].realFrameRate.toString()
                            if (rate== null || rate == "0/0")
                            {
                                rate = mediaInformation.mediaInformation.streams[1].realFrameRate.toString()
                            }
                            // Check if the frame rate is above 60
                            if (isFrameRateAbove60(rate)) {
                                val msg1 = getString(R.string.input_rate_check)
                                AlertDialog.Builder(requireActivity()).apply {
                                    msg1
                                    setMessage(msg1).setPositiveButton(
                                        "OK"
                                    ) { _, _ -> (requireActivity()) }
                                }.create().show()
                            }
                            videoHeight = mediaInformation.mediaInformation.streams[0].height.toString()
                            videoWidth = mediaInformation.mediaInformation.streams[0].width.toString()
                            var side = mediaInformation.mediaInformation.streams[0].getStringProperty("side_data_list")
                            when (videoHeight){
                                "null" ->{
                                    videoHeight = mediaInformation.mediaInformation.streams[1].height.toString()
                                    videoWidth = mediaInformation.mediaInformation.streams[1].width.toString()
                                    side = mediaInformation.mediaInformation.streams[1].getStringProperty("side_data_list")
                                }
                            }
                            //check if video is rotated and swap resolution
                            if (side != null) {
                                val rotation = side.substringAfter("rotation\":").substringBefore('}')
                                if (rotation == "-90" || rotation == "-270" || rotation == "90"){
                                    videoHeight = mediaInformation.mediaInformation.streams[0].width.toString()
                                    videoWidth = mediaInformation.mediaInformation.streams[0].height.toString()
                                    when (videoHeight){
                                        "null" ->{
                                            videoHeight = mediaInformation.mediaInformation.streams[1].width.toString()
                                            videoWidth = mediaInformation.mediaInformation.streams[1].height.toString()
                                        }
                                    }
                                }

                            }



                            videoResolution= videoWidth + "x" + videoHeight
                            videoResolutionInit = videoResolution


                        }


                    // Configure VideoView and make the controls visible when it's ready
                        binding.videoView.visibility = View.VISIBLE
                        binding.videoView.setVideoURI(uri)

                    // Listener to know when the video is ready to play
                        binding.videoView.setOnPreparedListener {

                            val mediaController = MediaController(requireActivity())

                            mediaController.setAnchorView(binding.videoView)

                            mediaController.scaleY = 0.5f  // vertical scale, 0.5 times its original size
                            mediaController.scaleX = 0.5f  // horizontal scale, 0.5 times its original size


                    // Apply customized background
                            mediaController.setBackgroundResource(R.drawable.rounded_media_controller)

                            binding.videoView.setMediaController(mediaController)
                            binding.videoView.start()
                            mediaController.show(0)
                            // Forze MediaController disappear after 3 seconds (3000 ms)

                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaController.hide()  // Hide MediaController after 3 seconds
                            }, 3000)  // Timne in miliseconds
                        }


                        initialSize = fileSize(videoUrl!!.length(requireActivity().contentResolver))

                        var initS=0.0
                        if (initialSize != "") {
                            val initSize = initialSize.substringBefore(" ")
                            val init = initSize.replace(",",".").toDouble()

                            if (initialSize.contains("M") )
                            {
                                initS=init*1000000
                            }
                            if (initialSize.contains("G") )
                            {
                                initS=init*1000000000

                            }
                            if (initialSize.contains("k") )
                            {
                                initS=init*1000

                            }

                        }
                        init75=initS*(1-0.75)
                        init40=initS*(1-0.4)
                        init50=initS*(1-0.5)
                        init70=initS*(1-0.7)

                        if (initialSize != "") {


                            if (initialSize.contains("M") )
                            {
                                init75 /= 1000000
                                init50 /= 1000000
                                init40 /= 1000000
                                init70 /= 1000000
                                unidades75 = "MB"
                                unidades40 = "MB"
                                unidades50 = "MB"
                                unidades70 = "MB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades75 = "KB"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades40 = "KB"
                                }
                                if (init50<1){
                                    init50 *= 1000
                                    unidades50 = "KB"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades70 = "KB"
                                }
                            }
                            if (initialSize.contains("G") )
                            {
                                init75 /= 1000000000
                                init40 /= 1000000000
                                init50 /= 1000000000
                                init70 /= 1000000000
                                unidades75 = "GB"
                                unidades40 = "GB"
                                unidades70 = "GB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades75 = "MB"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades40 = "MB"
                                }
                                if (init50<1){
                                    init50 *= 1000
                                    unidades50 = "MB"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades70 = "MB"
                                }

                            }
                            if (initialSize.contains("k") )
                            {
                                init75 /= 1000
                                init40 /= 1000
                                init50 /= 1000
                                init70 /= 1000
                                unidades75 = "KB"
                                unidades40 = "KB"
                                unidades70 = "KB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades75 = "B"
                                }
                                if (init50<1){
                                    init50 *= 1000
                                    unidades50 = "B"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades40 = "B"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades70 = "B"
                                }
                            }

                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireActivity(), "Error", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }

            }
        }


    companion object {
        fun newInstance() = HomeFragment()

        const val URI_PATH = "uri_path"
        const val RETURN_CODE = "return_code"
        const val INITIAL_SIZE = "initial_size"
        const val COMPRESS_SZE = "compressed_size"
        const val CONVERSION_TIME = "conversion_time"
        const val INITIAL_BATTERY = "initial_battery"
        const val REMAINING_BATTERY = "remaining_battery"
        const val CO2 = "co2"
    }


}