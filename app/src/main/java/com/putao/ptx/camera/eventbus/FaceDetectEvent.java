package com.putao.ptx.camera.eventbus;

import com.putao.ptx.camera.model.FaceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/6/14.
 */
public class FaceDetectEvent {
    public List<FaceModel> faces = new ArrayList<>();
}
