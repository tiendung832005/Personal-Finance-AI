package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.goal.CreateGoalRequest;
import com.data.personalfinanceinsightai.dto.request.goal.UpdateGoalRequest;
import com.data.personalfinanceinsightai.dto.response.goal.GoalDetailResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalProgressResponse;
import com.data.personalfinanceinsightai.dto.response.goal.GoalResponse;
import java.util.List;

public interface GoalService {

    GoalResponse create(String email, CreateGoalRequest request);

    List<GoalResponse> listForUser(String email);

    GoalDetailResponse getById(String email, Long id);

    GoalResponse update(String email, Long id, UpdateGoalRequest request);

    void delete(String email, Long id);

    GoalProgressResponse getProgress(String email, Long id);
}
