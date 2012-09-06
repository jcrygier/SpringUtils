package com.crygier.spring.util.web

import javax.servlet.http.HttpServletRequest

import org.springframework.context.support.StaticApplicationContext
import org.springframework.oxm.xstream.XStreamMarshaller
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.View
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
}
