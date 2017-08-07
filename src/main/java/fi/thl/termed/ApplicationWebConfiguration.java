package fi.thl.termed;

import com.google.gson.Gson;
import fi.thl.termed.util.csv.GsonCsvMessageConverter;
import fi.thl.termed.util.jena.JenaModelMessageConverter;
import fi.thl.termed.util.rdf.RdfMediaTypes;
import fi.thl.termed.util.spring.http.MediaTypes;
import fi.thl.termed.util.xml.GsonXmlMessageConverter;
import fi.thl.termed.web.external.node.transform.NodeDtoListRdfMessageConverter;
import fi.thl.termed.web.external.node.transform.NodeDtoRdfMessageConverter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class ApplicationWebConfiguration extends WebMvcConfigurerAdapter {

  @Autowired
  private Gson gson;

  @Value("${fi.thl.termed.baseUri:http://termed.thl.fi/api}")
  private String baseUri;

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer config) {
    config
        .favorParameter(true)
        .favorPathExtension(true)
        .mediaType("json", MediaType.APPLICATION_JSON_UTF8)
        .mediaType("xml", MediaTypes.TEXT_XML)
        .mediaType("csv", MediaTypes.TEXT_CSV)
        .mediaType("jsonld", RdfMediaTypes.LD_JSON)
        .mediaType("rdf", RdfMediaTypes.RDF_XML)
        .mediaType("ttl", RdfMediaTypes.TURTLE)
        .mediaType("n3", RdfMediaTypes.N3)
        .mediaType("nt", RdfMediaTypes.N_TRIPLES);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
    gsonHttpMessageConverter.setGson(gson);
    converters.addAll(Arrays.asList(
        new JenaModelMessageConverter(),
        new NodeDtoRdfMessageConverter(baseUri),
        new NodeDtoListRdfMessageConverter(baseUri),
        new GsonXmlMessageConverter(gson),
        new GsonCsvMessageConverter(gson),
        gsonHttpMessageConverter));
    super.configureMessageConverters(converters);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    // don't split query strings by commas
    registry.removeConvertible(String.class, Collection.class);
  }

}
