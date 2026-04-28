package com.sky.service;

import com.sky.vo.DishVO;

import java.util.List;

public interface SceneRecommendService {

    List<DishVO> getSceneRecommend(Long categoryId);

    String getCurrentScene();
}
