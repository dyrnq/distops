package com.dyrnq.distops.controller;


import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.i18n.annotation.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@I18n
public class IndexController {
    static Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Mapping("/")
    public Object index(Context ctx) {
        ModelAndView model = new ModelAndView("index.html");
        return model;
    }
}
