package com.heima.wemedia.controller.v1;

import com.heima.model.admin.dtos.ManualReviewPageDto;
import com.heima.model.admin.dtos.WmNewsManualReviewDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;

    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmNewsPageReqDto dto) {
        return wmNewsService.findList(dto);
    }

    @PostMapping("/submit")
    public ResponseResult submitNews(@RequestBody WmNewsDto dto){
        return wmNewsService.submitNews(dto);
    }

    @GetMapping("/one/{id}")
    public ResponseResult getArticle(@PathVariable("id") Integer id){
        return wmNewsService.getArticle(id);
    }

    @GetMapping("/del_news/{id}")
    public ResponseResult delArticle(@PathVariable("id") Integer id){
        return wmNewsService.delArticle(id);
    }

    @PostMapping("/down_or_up")
    public ResponseResult downOrUpArticle(@RequestBody WmNewsDto dto){
        return wmNewsService.downOrUpArticle(dto);
    }

    @PostMapping("/list_vo")
    public ResponseResult listManualReview(@RequestBody ManualReviewPageDto dto){
        return wmNewsService.listManualReview(dto);
    }
    @GetMapping("/one_vo/{id}")
    public ResponseResult getArticleVo(@PathVariable("id") Integer id){
        return wmNewsService.getArticle(id);
    }
    @PostMapping("/auth_fail")
    public ResponseResult manualReviewFailed(@RequestBody WmNewsManualReviewDto dto){
        return wmNewsService.manualReviewFailed(dto);
    }
    @PostMapping("/auth_pass")
    public ResponseResult manualReviewPassed(@RequestBody WmNewsManualReviewDto dto){
        return wmNewsService.manualReviewPassed(dto);
    }
}
