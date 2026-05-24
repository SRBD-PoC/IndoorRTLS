package com.example.indoorrtls.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.example.indoorrtls.ui.main.MainViewModel;
import com.example.indoorrtls.utils.AdvertiseUtil;

import java.util.Objects;
import java.util.stream.Stream;

public class Position extends View {

    private static final String TAG = "Position";

    public Position(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMainViewModel(MainViewModel mainViewModel, LifecycleOwner lifecycleOwner) {
        mainViewModel.getBluetoothScanResults().observe(lifecycleOwner, scanResults -> {
            Stream<AdvertiseUtil.ParsedPayload> others = scanResults.stream()
                    .map(AdvertiseUtil::parseScanResult)
                    .filter(Objects::nonNull);

            AdvertiseUtil.ParsedPayload self = AdvertiseUtil.createSelfPayload();

            Stream<AdvertiseUtil.ParsedPayload> parsedPayloads = Stream.concat(Stream.of(self), others);


            // Redraw or update UI with parsedPayloads
            invalidate();
        });
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

    }
}
