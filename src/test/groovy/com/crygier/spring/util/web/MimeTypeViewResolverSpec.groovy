/**
 * Copyright 2012 John Crygier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crygier.spring.util.web

import javax.servlet.http.HttpServletRequest

import org.springframework.context.support.StaticApplicationContext
import org.springframework.oxm.xstream.XStreamMarshaller
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.BeanNameViewResolver
import org.springframework.web.servlet.view.InternalResourceView
import org.springframework.web.servlet.view.json.MappingJacksonJsonView
import org.springframework.web.servlet.view.xml.MarshallingView

import spock.lang.Specification

// Light on the test for now - since it's quite dependent on the Spring MVC API
class MimeTypeViewResolverSpec extends Specification {
	
	def createResolver(def viewNameFromRequest, def mimeType, Closure withResolver = null) {
		def request = [getHeader : { mimeType }] as HttpServletRequest
		
		RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);
		
		def testResolver = new MimeTypeViewResolver() {
			protected String getViewNameFromRequest() throws Exception {
				return viewNameFromRequest;
			}
		}
		testResolver.setApplicationContext(new StaticApplicationContext())
		if (withResolver != null)
			withResolver.call(testResolver)
		testResolver.afterPropertiesSet()
		
		return testResolver;
	}
	
	def "no configuration html"() {
		when:
		def viewResolver = createResolver("testView", "text/html")
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
		v instanceof InternalResourceView
		v.url == "/testView.jsp"
	}
	
	def "html with override defaults"() {
		when:
		def viewResolver = createResolver("testView", "text/html", { r ->
			r.setDefaultInternalResourcePrefix("/WEB-INF/")
			r.setDefaultInternalResourceSuffix(".do")
		})
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
		v instanceof InternalResourceView
		v.url == "/WEB-INF/testView.do"
	}
	
	def "no configuration json"() {
		when:
		def viewResolver = createResolver("testView", "application/json")
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
		v instanceof MappingJacksonJsonView
	}
	
	def "no configuration xml"() {
		when:
		def viewResolver = createResolver("testView", "text/xml")
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
		v instanceof MarshallingView
		v.marshaller instanceof XStreamMarshaller
	}
	
	def "accepts header everything - ensure default"() {
		when:
		def viewResolver = createResolver("testView", "*/*")
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
		v instanceof InternalResourceView
		v.url == "/testView.jsp"
	}
	
	def "accepts header everything - change default"() {
		when:
		def viewResolver = createResolver("testView", "*/*")
		viewResolver.setDefaultResolver([resolveViewName : { it1, it2 -> [  ] as View }] as ViewResolver);
		
		then:
		View v = viewResolver.resolveViewName("testView", Locale.getDefault())
		v
	}
}
