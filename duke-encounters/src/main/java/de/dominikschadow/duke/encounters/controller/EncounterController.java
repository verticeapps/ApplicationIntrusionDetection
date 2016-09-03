/*
 * Copyright (C) 2016 Dominik Schadow, dominikschadow@gmail.com
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
package de.dominikschadow.duke.encounters.controller;

import de.dominikschadow.duke.encounters.domain.Encounter;
import de.dominikschadow.duke.encounters.services.EncounterService;
import de.dominikschadow.duke.encounters.services.UserService;
import de.dominikschadow.duke.encounters.validators.EncounterValidator;
import org.apache.commons.lang3.StringUtils;
import org.owasp.appsensor.core.DetectionPoint;
import org.owasp.appsensor.core.DetectionSystem;
import org.owasp.appsensor.core.Event;
import org.owasp.appsensor.core.event.EventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

import static org.owasp.appsensor.core.DetectionPoint.Category.REQUEST;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller to handle all encounter related requests.
 *
 * @author Dominik Schadow
 */
@Controller
public class EncounterController {
    private EncounterService encounterService;
    private EncounterValidator validator;
    private UserService userService;
    private DetectionSystem detectionSystem;
    private EventManager ids;

    @Autowired
    public EncounterController(final EncounterService encounterService, final EncounterValidator validator,
                               final UserService userService, final DetectionSystem detectionSystem,
                               final EventManager ids) {
        this.encounterService = encounterService;
        this.validator = validator;
        this.userService = userService;
        this.detectionSystem = detectionSystem;
        this.ids = ids;
    }

    @RequestMapping(value = "/encounters", method = GET)
    public String getEncounters(final Model model, @RequestParam(name = "type", required = false) final String type) {
        boolean confirmable = !StringUtils.equals(userService.getUser().getUsername(), "anonymousUser")
                && !StringUtils.equals("own", type);

        List<Encounter> encounters = encounterService.getEncounters(type);
        model.addAttribute("encounters", encounters);
        model.addAttribute("confirmable", confirmable);

        return "encounters";
    }

    @RequestMapping(value = "/encounter/create", method = GET)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public String createEncounter(@ModelAttribute final Encounter encounter) {
        return "user/createEncounter";
    }

    @RequestMapping(value = "/encounter/create", method = POST)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public String saveEncounter(@Valid final Encounter encounter, final Model model, final BindingResult result) {
        if (result.hasErrors()) {
            model.addAttribute("formErrors", result.getAllErrors());
            return "user/createEncounter";
        }

        encounterService.createEncounter(encounter);
        model.addAttribute("confirmable", true);

        return "redirect:/encounters";
    }

    @RequestMapping(value = "/encounter/delete", method = POST)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ModelAndView deleteEncounter(final long encounterId) {
        encounterService.deleteEncounter(encounterId);

        return new ModelAndView("redirect:/account");
    }

    @RequestMapping(value = "/encounter/{id}", method = GET)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public String encounterById(@PathVariable("id") final long encounterId, final Model model,
                                final RedirectAttributes redirectAttributes) {
        Encounter encounter = encounterService.getEncounterById(encounterId);

        if (encounter == null) {
            fireInvalidUrlParameterEvent();
            redirectAttributes.addFlashAttribute("encounterFailure", true);
            redirectAttributes.addFlashAttribute("confirmable", true);

            return "redirect:/encounters";
        }

        model.addAttribute("encounter", encounter);

        return "user/encounterDetails";
    }

    private void fireInvalidUrlParameterEvent() {
        DetectionPoint detectionPoint = new DetectionPoint(REQUEST, "RE8-001");
        ids.addEvent(new Event(userService.getUser(), detectionPoint, detectionSystem));
    }

    @InitBinder
    protected void initBinder(final WebDataBinder binder) {
        binder.setValidator(validator);
    }
}
