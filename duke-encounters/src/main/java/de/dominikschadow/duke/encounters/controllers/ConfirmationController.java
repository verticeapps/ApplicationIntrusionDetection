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
package de.dominikschadow.duke.encounters.controllers;

import de.dominikschadow.duke.encounters.domain.Confirmation;
import de.dominikschadow.duke.encounters.services.ConfirmationService;
import de.dominikschadow.duke.encounters.services.EncounterService;
import de.dominikschadow.duke.encounters.services.UserService;
import org.owasp.appsensor.core.DetectionPoint;
import org.owasp.appsensor.core.DetectionSystem;
import org.owasp.appsensor.core.Event;
import org.owasp.appsensor.core.event.EventManager;
import org.owasp.security.logging.SecurityMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.owasp.appsensor.core.DetectionPoint.Category.INPUT_VALIDATION;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller to handle all encounter confirmation related requests.
 *
 * @author Dominik Schadow
 */
@Controller
public class ConfirmationController {
    private static final Logger logger = LoggerFactory.getLogger(ConfirmationController.class);

    private ConfirmationService confirmationService;
    private EncounterService encounterService;
    private UserService userService;
    private DetectionSystem detectionSystem;
    private EventManager ids;

    @Autowired
    public ConfirmationController(ConfirmationService confirmationService, EncounterService encounterService, UserService userService, DetectionSystem detectionSystem, EventManager ids) {
        this.confirmationService = confirmationService;
        this.encounterService = encounterService;
        this.userService = userService;
        this.detectionSystem = detectionSystem;
        this.ids = ids;
    }

    @RequestMapping(value = "/confirmations", method = GET)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public String getConfirmations(Model model, @RequestParam(name = "type", required = false) String type) {
        List<Confirmation> confirmations = confirmationService.getConfirmations(type);
        model.addAttribute("confirmations", confirmations);

        return "user/confirmations";
    }

    @RequestMapping(value = "/confirmation/add", method = POST)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ModelAndView addConfirmation(long encounterId, RedirectAttributes redirectAttributes) {
        String username = userService.getUsername();

        if (encounterService.isOwnEncounter(encounterId, username)) {
            logger.info(SecurityMarkers.SECURITY_FAILURE, "User {} is owner of encounter {} and tried to confirm it",
                    username, encounterId);

            fireConfirmationErrorEvent();
            redirectAttributes.addFlashAttribute("ownEncounter", true);
        } else if (confirmationService.hasConfirmedEncounter(username, encounterId)) {
            logger.info(SecurityMarkers.SECURITY_FAILURE, "User {} has already confirmed encounter {} and tried to " +
                    "confirm it again", username, encounterId);

            fireConfirmationErrorEvent();
            redirectAttributes.addFlashAttribute("secondConfirm", true);
        } else {
            confirmationService.addConfirmation(username, encounterId);

            logger.info(SecurityMarkers.SECURITY_SUCCESS, "User {} confirmed encounter {}", username, encounterId);
        }

        return new ModelAndView("redirect:/account");
    }

    @RequestMapping(value = "/confirmation/revoke", method = POST)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ModelAndView revokeConfirmation(long confirmationId) {
        String username = userService.getUsername();

        confirmationService.deleteConfirmation(username, confirmationId);

        logger.info(SecurityMarkers.SECURITY_SUCCESS, "User {} revoked confirmation {}", username, confirmationId);

        return new ModelAndView("redirect:/account");
    }

    private void fireConfirmationErrorEvent() {
        DetectionPoint detectionPoint = new DetectionPoint(INPUT_VALIDATION, "IE5-001");
        ids.addEvent(new Event(userService.getUser(), detectionPoint, detectionSystem));
    }
}
