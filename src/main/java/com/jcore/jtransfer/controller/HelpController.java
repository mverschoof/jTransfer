package com.jcore.jtransfer.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HelpController {
 
	@RequestMapping(value = "/help", method = RequestMethod.GET)
	public String help(HttpServletRequest httpRequest, Model model) {
		return "/help";
	}
}
