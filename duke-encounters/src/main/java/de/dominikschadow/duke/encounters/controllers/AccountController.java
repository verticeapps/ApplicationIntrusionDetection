/*
 * Copyright (C) 2015 Dominik Schadow, dominikschadow@gmail.com
 *
 * This file is part of the Application Intrusion Detection project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dominikschadow.duke.encounters.controllers;

import de.dominikschadow.duke.encounters.domain.Confirmation;
import de.dominikschadow.duke.encounters.domain.DukeEncountersUser;
import de.dominikschadow.duke.encounters.domain.Encounter;
import de.dominikschadow.duke.encounters.services.ConfirmationService;
import de.dominikschadow.duke.encounters.services.EncounterService;
import de.dominikschadow.duke.encounters.services.UserService;
import de.dominikschadow.duke.encounters.validators.DukeEncountersUserValidator;
import org.owasp.security.logging.SecurityMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller to handle all account related requests.
 *
 * @author Dominik Schadow
 */
@Controller
public class AccountController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private EncounterService encounterService;
    @Autowired
    private ConfirmationService confirmationService;
    @Autowired
    private UserService userService;
    @Autowired
    private DukeEncountersUserValidator dukeEncountersUserValidator;

    @RequestMapping(value = "/account", method = GET)
    public String showMyAccount(Model model) {
        String username = userService.getUsername();

        LOGGER.info(SecurityMarkers.SECURITY_AUDIT, "User {} is accessing his account", username);

        List<Encounter> encounters = encounterService.getEncountersByUsername(username);
        model.addAttribute("encounters", encounters);

        List<Confirmation> confirmations = confirmationService.getConfirmationsByUsername(username);
        model.addAttribute("confirmations", confirmations);

        DukeEncountersUser dukeEncountersUser = userService.getDukeEncountersUser(username);
        model.addAttribute("userlevel", dukeEncountersUser.getLevel().getName());

        return "/user/account";
    }

    @RequestMapping(value = "/account/edit", method = GET)
    public String editMyAccount(Model model) {
        String username = userService.getUsername();

        LOGGER.info(SecurityMarkers.SECURITY_AUDIT, "User {} is editing his account", username);

        DukeEncountersUser user = userService.getDukeEncountersUser();
        model.addAttribute("user", user);

        DukeEncountersUser dukeEncountersUser = userService.getDukeEncountersUser(username);
        model.addAttribute("userlevel", dukeEncountersUser.getLevel().getName());

        return "/user/editAccount";
    }

    /**
     * Shows the registration page.
     *
     * @param dukeEncountersUser The new DukeEncountersUser
     * @return Register URL
     */
    @RequestMapping(value = "/register", method = GET)
    public String register(@ModelAttribute DukeEncountersUser dukeEncountersUser) {
        return "/register";
    }

    /**
     * Creates the new user and stored it in the database.
     *
     * @param dukeEncountersUser The new user to register
     * @return Login URL
     */
    @RequestMapping(value = "/register", method = POST)
    public ModelAndView createUser(@Valid DukeEncountersUser dukeEncountersUser, BindingResult result) {
        if (result.hasErrors()) {
            return new ModelAndView("/register", "formErrors", result.getAllErrors());
        }

        DukeEncountersUser newUser = userService.createUser(dukeEncountersUser);

        LOGGER.info(SecurityMarkers.SECURITY_AUDIT, "User {} created", newUser);

        ModelAndView modelAndView = new ModelAndView("/login");
        modelAndView.addObject("userCreated", "User " + newUser.getUsername() + " successfully created, you can now " +
                "use your new user to log in.");

        return modelAndView;
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.setValidator(dukeEncountersUserValidator);
    }
}
