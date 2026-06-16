package com.trekmate.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forward all non-API, non-static requests to the React SPA entry point.
 */
@Controller
public class FrontendController {

    @GetMapping({
            "/",
            "/{path1:[^\\.]*}",
            "/{path1:[^\\.]*}/{path2:[^\\.]*}",
            "/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
