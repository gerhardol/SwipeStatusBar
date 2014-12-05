package cz.chladek.android.preferences;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import cz.chladek.swipe_status_bar.R;

public class BlinkingPreference extends Preference {

	private ValueAnimator animator;
	private int startColor, endColor, duration, repeatCount, delay;
	private View view;

	public BlinkingPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public BlinkingPreference(Context context) {
		this(context, null, android.R.attr.preferenceStyle);
	}

	public BlinkingPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BlinkingPreference, defStyle, 0);
			for (int i = a.getIndexCount(); i >= 0; i--) {
				int attr = a.getIndex(i);
				switch (attr) {
				case R.styleable.BlinkingPreference_startColor:
					startColor = a.getColor(attr, 0xff000000);
					break;
				case R.styleable.BlinkingPreference_endColor:
					endColor = a.getColor(attr, 0xffffffff);
					break;
				case R.styleable.BlinkingPreference_duration:
					duration = a.getInt(attr, 1000);
					break;
				case R.styleable.BlinkingPreference_repeatCount:
					repeatCount = a.getInt(attr, ValueAnimator.INFINITE);
					break;
				case R.styleable.BlinkingPreference_delay:
					delay = a.getInt(attr, 0);
					break;
				}
			}
			a.recycle();
		}
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		if (this.view != view) {
			animator = ObjectAnimator.ofInt(view, "backgroundColor", startColor, endColor);
			animator.setDuration(duration);
			animator.setEvaluator(new ArgbEvaluator());
			animator.setRepeatCount(repeatCount);
			animator.setStartDelay(delay);
			animator.setRepeatMode(ValueAnimator.REVERSE);
			animator.start();
		}
		this.view = view;
	}

	public void startBlinking() {
		if (animator != null)
			animator.start();
	}

	public void startReverseBlinking() {
		if (animator != null)
			animator.reverse();
	}
}
