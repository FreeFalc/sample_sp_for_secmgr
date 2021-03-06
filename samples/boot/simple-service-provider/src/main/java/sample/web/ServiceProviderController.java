/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package sample.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml.SamlObjectResolver;
import org.springframework.security.saml.config.ExternalProviderConfiguration;
import org.springframework.security.saml.saml2.SamlWsClient;
import org.springframework.security.saml.saml2.authentication.AuthzDecisionQueryRequest;
import org.springframework.security.saml.saml2.authentication.AuthzDecisionStatement.Action;
import org.springframework.security.saml.saml2.authentication.Response;
import org.springframework.security.saml.saml2.authentication.StatusCode;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.PolicyDecisionProviderMetadata;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml.spi.DefaultSamlTransformer;
import org.springframework.security.saml.spi.Defaults;
import org.springframework.security.saml.util.Network;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import sample.config.AppConfig;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
public class ServiceProviderController {

	private AppConfig configuration;
	private SamlObjectResolver resolver;
	private Network network;

  private Defaults defaults;
  private DefaultSamlTransformer samlTransformer;
	private SamlWsClient samlWsClient;

	@Autowired
	public void setNetwork(Network network) {
		this.network = network;
	}

	@Autowired
	public void setAppConfig(AppConfig config) {
		this.configuration = config;
	}

	@Autowired
	public void setMetadataResolver(SamlObjectResolver resolver) {
		this.resolver = resolver;
	}

	@Autowired
	public void setSamlWsClient(SamlWsClient samlWsClient) {
		this.samlWsClient = samlWsClient;
	}

	@Autowired
  public void setDefaults(Defaults defaults) {
    this.defaults = defaults;
  }

  @Autowired
  public void setDefaultSamlTransformer(DefaultSamlTransformer defaultSamlTransformer) {
	  this.samlTransformer = defaultSamlTransformer;
  }

	@RequestMapping(value = {"/", "/index", "logged-in"})
	public String home(HttpServletRequest request, Model model) {
    ServiceProviderMetadata localServiceProvider = resolver
        .getLocalServiceProvider(network.getBasePath(request));
    String idpId = (String) request.getSession().getAttribute("idp");
    PolicyDecisionProviderMetadata pdpMetadata = resolver.resolvePolicyDecisionProvider(idpId);
    Cookie gsaSessionIdCookie = Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equalsIgnoreCase("GSA_SESSION_ID")).findFirst().get();
    String gsaSessionId = gsaSessionIdCookie.getValue();

    List<String> resources = Arrays.asList(
        "https://www.google.com/",
        "http://http-authn:4444/resource1",
        "http://http-authn:4444/resource2");

    Map<String, String> resourceStatus = getDecisionForResources(localServiceProvider, pdpMetadata,
				gsaSessionId, resources);
    model.addAttribute("resources", resourceStatus);
    return "logged-in";
	}

	public Map<String, String> getDecisionForResources(ServiceProviderMetadata localServiceProvider,
			PolicyDecisionProviderMetadata pdpMetadata, String gsaSessionId, List<String> resources) {
		return resources.stream()
				.collect(Collectors.toMap(Function.identity(), resource -> {
					AuthzDecisionQueryRequest authzQueryRequest = defaults
							.authzDecisionQueryRequest(gsaSessionId, resource,
									Collections.singletonList(Action.HTTP_GET_ACTION), localServiceProvider);

					String authzQueryRequestXml = samlTransformer.toXml(authzQueryRequest);
					String authzEndpoint = pdpMetadata.getPolicyDecisionProvider().getAuthzService().get(0)
							.getLocation();
					Response authzResponse = samlWsClient.sendRequest(authzQueryRequestXml, authzEndpoint);
					String errorMessage = "";
					if (authzResponse.getStatus().getCode() != StatusCode.SUCCESS) {
						errorMessage = authzResponse.getStatus().getCode().toString();
						if (authzResponse.getStatus().getChildStatusCode() != null) {
							errorMessage += " / " + authzResponse.getStatus().getChildStatusCode().toString();
						}
					}
					String decisionMsg = authzResponse.getAssertions().get(0).getAuthzDecisionStatements().get(0)
							.getDecision().toString();

					if (!errorMessage.isEmpty()) {
						decisionMsg += "   /   " + errorMessage;
					}
					return decisionMsg;
				}));
	}

	@RequestMapping("/saml/sp/select")
	public String selectProvider(HttpServletRequest request, Model model) {
		List<ModelProvider> providers = new LinkedList<>();
		configuration.getServiceProvider().getProviders().stream().forEach(
			p -> {
				try {
					ModelProvider mp = new ModelProvider().setLinkText(p.getLinktext()).setRedirect(getDiscoveryRedirect(request, p));
					providers.add(mp);
				} catch (Exception x) {
					x.printStackTrace();
				}
			}
		);
		model.addAttribute("idps", providers);
		return "select-provider";
	}

	protected String getDiscoveryRedirect(HttpServletRequest request, ExternalProviderConfiguration p) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(network.getBasePath(request));
		builder.pathSegment("saml/sp/discovery");
		IdentityProviderMetadata metadata = resolver.resolveIdentityProvider(p);
		builder.queryParam("idp", UriUtils.encode(metadata.getEntityId(), UTF_8));
		return builder.build().toUriString();
	}
}
