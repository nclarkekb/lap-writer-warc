package dk.netarkivet.lap;

import javax.servlet.http.HttpServletRequest;

import com.antiaction.common.templateengine.login.LoginTemplateUser;

public class User implements LoginTemplateUser {

	@Override
	public String get_cookie_token(HttpServletRequest req) {
		return null;
	}

}
