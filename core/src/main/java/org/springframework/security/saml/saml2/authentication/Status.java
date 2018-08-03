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
package org.springframework.security.saml.saml2.authentication;

public class Status {

	private StatusCode code;
	private String message;
	private String detail;

	private StatusCode childStatusCode;

	public StatusCode getCode() {
		return code;
	}

	public Status setCode(StatusCode code) {
		this.code = code;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public Status setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public Status setDetail(String detail) {
		this.detail = detail;
		return this;
	}

	public StatusCode getChildStatusCode() {
		return childStatusCode;
	}

	public Status setChildStatusCode(StatusCode childStatusCode) {
		this.childStatusCode = childStatusCode;
		return this;
	}
}
