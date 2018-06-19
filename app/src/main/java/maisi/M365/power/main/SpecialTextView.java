package maisi.M365.power.main;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

public class SpecialTextView extends android.support.v7.widget.AppCompatTextView {
    private RequestType type;

    public SpecialTextView(Context context) {
        super(context);
    }

    public SpecialTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public SpecialTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public SpecialTextView(Context context, @Nullable AttributeSet attrs, RequestType type) {
        super(context, attrs);
        this.type = type;
    }

    public SpecialTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, RequestType type) {
        super(context, attrs, defStyleAttr);
        this.type = type;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }
}
