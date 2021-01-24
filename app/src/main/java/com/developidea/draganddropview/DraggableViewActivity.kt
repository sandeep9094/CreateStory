package com.developidea.draganddropview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.developidea.draganddropview.databinding.ActivityDraggableViewBinding
import com.developidea.draganddropview.databinding.DrawSketchToolsBinding
import com.developidea.draganddropview.databinding.EditStoryToolsBinding
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.jaygoo.widget.VerticalRangeSeekBar
import com.developidea.draganddropview.DrawingView
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import io.github.hyuwah.draggableviewlib.Draggable
import io.github.hyuwah.draggableviewlib.DraggableListener
import io.github.hyuwah.draggableviewlib.makeDraggable

class DraggableViewActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "DraggableViewActivity"
    }

    private lateinit var deleteView: ImageView
    private lateinit var addEmojiButton: ImageView
    private lateinit var addTextButton: ImageView
    private lateinit var drawSketchButton: ImageView
    private lateinit var colorPicker: ImageView

    private lateinit var drawingClear: ImageView
    private lateinit var drawingEraser: ImageView
    private lateinit var drawingPen: ImageView
    private lateinit var drawingDone: ImageView

    private lateinit var textSizeSlider: VerticalRangeSeekBar
    private lateinit var binding: ActivityDraggableViewBinding

    private var lastDrawPenSelectedColor: Int = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDraggableViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addEmojiButton = findViewById(R.id.add_emoji)
        addTextButton = findViewById(R.id.add_text)
        drawSketchButton = findViewById(R.id.add_sketch)
        deleteView = findViewById(R.id.delete_view)
        textSizeSlider = findViewById(R.id.text_size_slider)
        colorPicker = findViewById(R.id.color_picker_button)

        drawingClear = findViewById(R.id.drawing_clear)
        drawingEraser = findViewById(R.id.drawing_eraser)
        drawingPen = findViewById(R.id.drawing_pen)
        drawingDone = findViewById(R.id.drawing_done)

        addEmojiButton.setOnClickListener(this)
        addTextButton.setOnClickListener(this)
        drawSketchButton.setOnClickListener(this)

        drawingClear.setOnClickListener(this)
        drawingEraser.setOnClickListener(this)
        drawingPen.setOnClickListener(this)
        drawingDone.setOnClickListener(this)

        binding.openCameraButton.setOnClickListener(this)
        binding.penColorPickerButton.setOnClickListener(this)


        initDeleteView(deleteView)

    }

    private fun openCameraImagePickerIntent() {
        val FRONT_CAMERA = 0
        val BACK_CAMERA = 1
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra("android.intent.extras.CAMERA_FACING", BACK_CAMERA)
        startActivityForResult(intent, 14596)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 14596 && resultCode == RESULT_OK) {
            data?.let {
                val imageBitmap = data.extras!!["data"] as Bitmap?
                binding.imagePreview.setImageBitmap(imageBitmap)
                //Save imageBitmap in storage
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.open_camera_button -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                openCameraImagePickerIntent()
            }
            R.id.add_text -> {
                addNewTextField(view.context)
            }
            R.id.add_sketch -> {
                addDrawingPad()
            }
            R.id.drawing_pen -> {
                binding.sketchDrawingView.penColor = lastDrawPenSelectedColor
                binding.sketchDrawingView.initializePen()
            }
            R.id.drawing_eraser -> {
                binding.sketchDrawingView.initializeEraser()
            }
            R.id.drawing_clear -> {
                binding.sketchDrawingView.clear()
                binding.editToolsLayout.makeVisible()
                binding.sketchToolsLayout.makeGone()
                binding.penSizeSelector.makeGone()
                binding.penColorPickerButton.makeGone()
            }
            R.id.drawing_done -> {
                binding.editToolsLayout.makeVisible()
                binding.sketchToolsLayout.makeGone()
                binding.penSizeSelector.makeGone()
                binding.penColorPickerButton.makeGone()
                binding.sketchDrawingView.penColor = Color.TRANSPARENT
            }
            R.id.pen_color_picker_button -> {
                showDrawingPenColorPicker(binding.sketchDrawingView)
            }
        }
    }

    private fun addNewTextField(context: Context) {
        val textField = EditText(context)
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
//        params.weight = 1.0f
        params.gravity = Gravity.CENTER
        textField.layoutParams = params
//        binding.frameLayout.layoutParams = params
        binding.frameLayout.addView(textField)
        textField.setTextColor(resources.getColor(android.R.color.white))
        textField.imeOptions = EditorInfo.IME_ACTION_DONE
        textField.setRawInputType(InputType.TYPE_CLASS_TEXT)
        textField.background.clearColorFilter()
        textField.backgroundTintMode = PorterDuff.Mode.CLEAR
        textField.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocusEditText(context, textField)
                makeViewDraggable(textField)
                return@setOnEditorActionListener true
            }
            false
        }
        textField.setOnClickListener {
            makeFocusableEditText(this@DraggableViewActivity, textField)
            initTextFieldTools(textField)
        }

        textField.performClick()
    }

    private fun initDeleteView(deleteView: ImageView) {
        deleteView.setOnHoverListener { view, motionEvent ->
            Log.d(TAG, "Delete View is hover : $motionEvent.h")
            return@setOnHoverListener true
        }
        deleteView.setOnFocusChangeListener { view, focusable ->
            Log.d(TAG, "Delete View is focusable : $focusable")
        }
    }

    private fun makeFocusableEditText(context: Context, editText: EditText) {
        editText.isFocusable = true
        editText.requestFocus()
        val imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        showTextFieldTools()
    }

    private fun clearFocusEditText(context: Context, editText: EditText) {
        editText.clearFocus()
        hideKeyboard(context, editText)
        hideTextFieldTools()
    }

    private fun makeViewDraggable(view: View?) {
        view!!.makeDraggable(Draggable.STICKY.NONE, true, object : DraggableListener {
            override fun onPositionChanged(view: View) {

            }
        })
    }

    private fun initTextFieldTools(editText: EditText) {
        textSizeSlider.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                try {
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, leftValue)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }

            override fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
            }

            override fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
            }

        })

        colorPicker.setOnClickListener {
            showColorPicker(editText)
        }
    }

    private fun showTextFieldTools() {
        textSizeSlider.makeVisible()
        colorPicker.makeVisible()
    }

    private fun hideTextFieldTools() {
        textSizeSlider.makeGone()
        colorPicker.makeGone()
    }

    private fun showColorPicker(editText: EditText) {
        ColorPickerDialog.Builder(this)
                .setTitle("Select Color")
                .setPreferenceName("colorPicker")
                .setPositiveButton("Confirm",
                        ColorEnvelopeListener { envelope, fromUser ->
                            editText.setTextColor(envelope.color)
                        })
                .setNegativeButton("Cancel")
                { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(true) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .show()
    }

    private fun showDrawingPenColorPicker(drawingView: DrawingView) {
        ColorPickerDialog.Builder(this)
                .setTitle("Select Color")
                .setPreferenceName("drawingPenColor")
                .setPositiveButton("Confirm",
                        ColorEnvelopeListener { envelope, fromUser ->
                            drawingView.penColor = envelope.color
                            lastDrawPenSelectedColor = envelope.color
                            drawingView.initializePen()
                        })
                .setNegativeButton("Cancel")
                { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(true) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .show()
    }

    private fun addDrawingPad() {
        if (!binding.sketchDrawingView.isVisible()) {
            //For first time init because last set properties being override
            binding.sketchDrawingView.makeVisible()
            binding.penSizeSelector.makeVisible()
            binding.penColorPickerButton.makeVisible()
            binding.editToolsLayout.makeGone()
            binding.sketchToolsLayout.makeVisible()
            binding.penSizeSelector.setOnRangeChangedListener(object : OnRangeChangedListener {
                override fun onRangeChanged(view: RangeSeekBar?, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                    binding.sketchDrawingView.penSize = leftValue
                    binding.sketchDrawingView.eraserSize = leftValue
                    drawingPen.performClick()
                    Log.d(TAG,"Drawing Pen Size : ${binding.sketchDrawingView.penSize}")
                }

                override fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {

                }

                override fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {

                }

            })
            drawingPen.performClick()
            return
        }

        binding.editToolsLayout.makeGone()
        binding.sketchToolsLayout.makeVisible()
        binding.penSizeSelector.makeVisible()
        binding.penColorPickerButton.makeVisible()
        drawingPen.performClick()
    }

}