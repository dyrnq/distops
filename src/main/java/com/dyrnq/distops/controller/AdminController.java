package com.dyrnq.distops.controller;


import com.dyrnq.distops.CfgExtractor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Path;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.i18n.annotation.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mapping("admin")
@Controller
@I18n
@Slf4j
public class AdminController extends BaseController {

    @Inject
    CfgExtractor cfgExtractor;

    @Mapping("inst")
    public Object inst() {
        ModelAndView model = new ModelAndView("admin/inst.html");
        return model;
    }

    @Mapping("instConfig-{id}")
    public Object instConfig(@Path("id") Long id ) {
        ModelAndView model = new ModelAndView("admin/instConfig.html");
        model.put("id", id);
        return model;
    }

    @Mapping("instEdit")
    public Object instEdit() {
        ModelAndView model = new ModelAndView("admin/instEdit.html");
        model.put("id", "");
        return model;
    }


    @Mapping("instEdit-{id}")
    public Object instEdit(@Path("id") Long id ) {
        ModelAndView model = new ModelAndView("admin/instEdit.html");
        model.put("id", id);
        return model;
    }

    @Mapping("artifact")
    public Object artifact() {
        ModelAndView model = new ModelAndView("admin/artifact.html");
        return model;
    }

    @Mapping("oci")
    public Object oci() {
        ModelAndView model = new ModelAndView("admin/oci.html");
        return model;
    }

    @Mapping("accountEdit")
    public Object accountEdit() {
        ModelAndView model = new ModelAndView("admin/accountEdit.html");
        model.put("id", "");
        return model;
    }

    @Mapping("accountEdit-{id}")
    public Object accountEdit(@Path("id") Long id) {
        ModelAndView model = new ModelAndView("admin/accountEdit.html");
        model.put("id", id);
        return model;
    }

    @Mapping("account")
    public Object account() {
        ModelAndView model = new ModelAndView("admin/account.html");
        return model;
    }

    @Mapping("repo")
    public Object repo() {
        ModelAndView model = new ModelAndView("admin/repo.html");
        return model;
    }




    @Mapping("userEdit")
    public Object userEdit() {
        ModelAndView model = new ModelAndView("admin/userEdit.html");
        model.put("id", "");
        return model;
    }

    @Mapping("userEdit-{id}")
    public Object userEdit(@Path("id") String id) {
        ModelAndView model = new ModelAndView("admin/userEdit.html");
        model.put("id", id);
        return model;
    }

    @Mapping("userPassEdit-{id}")
    public Object userPassEdit(@Path("id") String id) {
        ModelAndView model = new ModelAndView("admin/userPassEdit.html");
        model.put("id", id);
        return model;
    }

    @Mapping("user")
    public Object user() {
        ModelAndView model = new ModelAndView("admin/user.html");
        return model;
    }


    @Mapping("templateConfig")
    public Object templateConfig() {
        ModelAndView model = new ModelAndView("admin/templateConfig.html");
        return model;
    }
    @Mapping("about")
    public Object about() {
        ModelAndView model = new ModelAndView("admin/about.html");
        return model;
    }




    @Mapping("login")
    public Object login() {
        ModelAndView model = new ModelAndView("admin/login.html");
        return model;
    }


    @Mapping("")
    public Object index(Context ctx) {
        String token = ctx.cookie(cfgExtractor.tokenCookieName());

        if (Utils.isEmpty(token)) {
            ModelAndView model = new ModelAndView("admin/index-noauth.html");
            return model;
        } else {
            ModelAndView model = new ModelAndView("admin/index-auth.html");
            return model;
        }

    }


}
