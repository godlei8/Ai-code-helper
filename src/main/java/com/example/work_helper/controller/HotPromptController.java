package com.example.work_helper.controller;

import com.example.work_helper.hot.HotPromptItem;
import com.example.work_helper.hot.HotPromptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hot")
public class HotPromptController {

    private final HotPromptService hotPromptService;

    public HotPromptController(HotPromptService hotPromptService) {
        this.hotPromptService = hotPromptService;
    }

    @GetMapping("/prompts")
    public List<HotPromptItem> listPrompts(@RequestParam(defaultValue = "false") boolean refresh) {
        return hotPromptService.getDailyHotPrompts(refresh);
    }
}
