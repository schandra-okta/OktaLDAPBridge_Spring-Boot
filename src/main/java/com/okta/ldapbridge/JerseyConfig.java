package com.okta.ldapbridge;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(UserTypeResource.class);
		register(UpdateOktaGroupMembershipResource.class);
		register(UpdateUserResource.class);
	}

}