package com.idormy.sms.forwarder.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.utils.AnimationUtils;
import com.idormy.sms.forwarder.utils.CommonUtils;

public class ClearEditText extends RelativeLayout {
    private EditText myEdie;
    private ImageView ivEditClean;
    private ImageView ivEditEye;
    private boolean isChecked = true;
    private final Context mContext;
    private TypedArray mTypedArray;
    private boolean showClean = true;//清空图标是否显示，true:显示
    private boolean showEye = false;//密码可见图标是否显示，true:显示
    private int drawableLeft = -1;//是否显示输入框左侧图片
    private int drawableEyeOpen = R.drawable.clear_icon_eye_open;//可以看见密码小眼睛样式
    private int drawableEyeClose = R.drawable.clear_icon_eye_close;//不可见密码小眼睛样式
    private int drawableClean = R.drawable.clear_icon_close;//清除按钮图片
    private int cleanPadding = 0;//清除按钮padding边距
    private String hintStr;
    private String textStr;
    private int mTextColorHint = -1; //Color.LTGRAY
    private int mTextColor = -1;//Color.BLACK
    private int mTextSize = -1;
    private int mMaxLength = 2000;
    private int mMaxLines = 1;
    private int mInputType = 0;//输入类型，就做了不限制、数字、文本密码三种
    private boolean isInput = false;//输入1个字符后更改状态为true,保证小眼睛移动一次
    private boolean isHideClean = false;//输入字符后，清除了需要小眼睛归为，清除按钮隐藏

    public ClearEditText(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public ClearEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mTypedArray = mContext.obtainStyledAttributes(attrs, R.styleable.ClearEditText);
        initView();
    }

    public ClearEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mTypedArray = mContext.obtainStyledAttributes(attrs, R.styleable.ClearEditText);
        showClean = mTypedArray.getBoolean(R.styleable.ClearEditText_showClean, showClean);
        drawableClean = mTypedArray.getResourceId(R.styleable.ClearEditText_drawableClean, drawableClean);
        cleanPadding = mTypedArray.getDimensionPixelSize(R.styleable.ClearEditText_cleanPadding, cleanPadding);

        showEye = mTypedArray.getBoolean(R.styleable.ClearEditText_showEye, showEye);
        drawableLeft = mTypedArray.getResourceId(R.styleable.ClearEditText_drawableLeft, -1);
        drawableEyeClose = mTypedArray.getResourceId(R.styleable.ClearEditText_drawableEyeClose, drawableEyeClose);
        drawableEyeOpen = mTypedArray.getResourceId(R.styleable.ClearEditText_drawableEyeOpen, drawableEyeOpen);

        hintStr = mTypedArray.getString(R.styleable.ClearEditText_hint);
        textStr = mTypedArray.getString(R.styleable.ClearEditText_text);
        mTextColorHint = mTypedArray.getColor(R.styleable.ClearEditText_textColorHint, mTextColorHint);
        mTextColor = mTypedArray.getColor(R.styleable.ClearEditText_textColor, mTextColor);
        mTextSize = mTypedArray.getDimensionPixelSize(R.styleable.ClearEditText_textSize, mTextSize);
        mMaxLength = mTypedArray.getInteger(R.styleable.ClearEditText_maxLength, mMaxLength);
        mMaxLines = mTypedArray.getDimensionPixelSize(R.styleable.ClearEditText_maxLines, mMaxLines);
        mInputType = mTypedArray.getInteger(R.styleable.ClearEditText_inputType, mInputType);

        mTypedArray.recycle();
        initView();
    }

    // 初始化视图
    private void initView() {
        View view = View.inflate(getContext(), R.layout.edit_text_clear, null);
        ImageView ivLeftIcon = view.findViewById(R.id.iv_edit_left_icon);
        myEdie = view.findViewById(R.id.view_edit_show);
        ivEditClean = view.findViewById(R.id.iv_edit_clean);
        ivEditEye = view.findViewById(R.id.iv_edit_eye);

        myEdie.setHint(hintStr);
        if (mTextColorHint != -1) myEdie.setHintTextColor(mTextColorHint);
        myEdie.setText(textStr);
        if (mTextColor != -1) myEdie.setTextColor(mTextColor);
        myEdie.setMaxLines(mMaxLines);
        myEdie.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mMaxLength)});
        if (mTextSize != -1) {
            myEdie.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        } else {
            myEdie.setTextSize(15);
        }
        if (mInputType == 1) {
            myEdie.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if (mInputType == 2) {
            myEdie.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        } else {
            myEdie.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_TEXT);
        }
        if (showEye) {
            myEdie.setTransformationMethod(new AsteriskPasswordTransformationMethod());
        }
        if (showClean && showEye) {
            int left = myEdie.getPaddingLeft();
            int top = myEdie.getPaddingTop();
            int bottom = myEdie.getPaddingBottom();
            myEdie.setPadding(left, top, CommonUtils.dp2px(mContext, 90), bottom);
        } else if (!showClean && !showEye) {
            int left = myEdie.getPaddingLeft();
            int top = myEdie.getPaddingTop();
            int right = myEdie.getPaddingRight();
            int bottom = myEdie.getPaddingBottom();
            myEdie.setPadding(left, top, right, bottom);
        } else {
            int left = myEdie.getPaddingLeft();
            int top = myEdie.getPaddingTop();
            int bottom = myEdie.getPaddingBottom();
            myEdie.setPadding(left, top, CommonUtils.dp2px(mContext, 45), bottom);
        }

        myEdie.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    isHideClean = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && !isInput) {//输入字符大于0且只有一个字符时候显示清除按钮动画，小眼睛移动出位置给清除按钮使用
                    showEditClean();
                    moveEditEye();
                    isInput = true;
                } else if (s.length() == 0) {//无字符小眼睛归位
                    UndoEditEye();
                }
                if (s.length() == 0 & !isHideClean) {
                    hideEditClean();
                    isHideClean = true;
                    isInput = false;
                }
                if (onEditInputListener != null) {
                    onEditInputListener.input(getText());
                }
            }
        });

        setEditClean(showClean);
        ivEditClean.setOnClickListener(v -> myEdie.setText(""));
        ivEditClean.setImageResource(drawableClean);
        ivEditClean.setPadding(cleanPadding, cleanPadding, cleanPadding, cleanPadding);

        setEditEye(showEye);
        ivEditEye.setOnClickListener(v -> {
            if (isChecked) {
                // 输入一个对用户可见的密码
                myEdie.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                myEdie.setSelection(getText().length());
                ivEditEye.setImageResource(drawableEyeOpen);
                isChecked = false;
            } else {
                // 输入一个对用户不可见的密码
                myEdie.setTransformationMethod(new AsteriskPasswordTransformationMethod());
                myEdie.setSelection(getText().length());
                ivEditEye.setImageResource(drawableEyeClose);
                isChecked = true;
            }
        });
        if (drawableLeft != -1) {
            ivLeftIcon.setVisibility(View.VISIBLE);
            ivLeftIcon.setImageResource(drawableLeft);
        } else {
            ivLeftIcon.setVisibility(View.GONE);
        }
        view.setLayoutParams(new LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT));
        addView(view);
    }

    //密码不可见时候，使用*替换密码
    public static class AsteriskPasswordTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new PasswordCharSequence(source);
        }

        private static class PasswordCharSequence implements CharSequence {

            private final CharSequence mSource;

            public PasswordCharSequence(CharSequence source) {
                mSource = source; // Store char sequence
            }

            public char charAt(int index) {
                return '*'; // This is the important part
            }

            public int length() {
                return mSource.length(); // Return default
            }

            @NonNull
            public CharSequence subSequence(int start, int end) {
                return mSource.subSequence(start, end); // Return default
            }
        }

    }

    public String getText() {
        return myEdie.getText().toString().trim();
    }

    public void setText(String text) {
        myEdie.setText(text);
    }

    //代码设置是否显示清除按钮
    public void setEditClean(boolean isCanClose) {
        showClean = isCanClose;
    }

    //代码设置是否显示小眼睛
    public void setEditEye(boolean isCanSee) {
        showEye = isCanSee;
        if (showEye) {
            ivEditEye.setVisibility(View.VISIBLE);
        } else {
            ivEditEye.setVisibility(View.GONE);
        }
    }

    private void showEditClean() {
        if (showClean) {
            AnimationUtils.showAndHiddenCenterAnimation(ivEditClean, AnimationUtils.AnimationState.STATE_SHOW, 500);
        }
    }

    private void hideEditClean() {
        if (showClean) {
            AnimationUtils.showAndHiddenCenterAnimation(ivEditClean, AnimationUtils.AnimationState.STATE_HIDDEN, 500);
        }
    }

    private void moveEditEye() {
        if (showEye) {
            //关闭按钮的宽度
            int ivWidth = 35;
            ObjectAnimator.ofFloat(ivEditEye, "translationX", -CommonUtils.dp2px(mContext, ivWidth)).setDuration(500).start();
        }
    }

    private void UndoEditEye() {
        if (showEye) {
            ObjectAnimator.ofFloat(ivEditEye, "translationX", 0).setDuration(500).start();
        }
    }

    public OnEditInputListener onEditInputListener;

    public void setOnEditInputListener(OnEditInputListener listener) {
        onEditInputListener = listener;
    }

    //输入监听
    public interface OnEditInputListener {
        void input(String content);
    }

}