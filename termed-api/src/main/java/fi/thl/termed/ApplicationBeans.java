package fi.thl.termed;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import fi.thl.termed.dao.Dao;
import fi.thl.termed.dao.jdbc.JdbcClassDao;
import fi.thl.termed.dao.jdbc.JdbcClassPermissionsDao;
import fi.thl.termed.dao.jdbc.JdbcClassPropertyValueDao;
import fi.thl.termed.dao.jdbc.JdbcPropertyDao;
import fi.thl.termed.dao.jdbc.JdbcPropertyPropertyValueDao;
import fi.thl.termed.dao.jdbc.JdbcReferenceAttributeDao;
import fi.thl.termed.dao.jdbc.JdbcReferenceAttributePermissionsDao;
import fi.thl.termed.dao.jdbc.JdbcReferenceAttributePropertyValueDao;
import fi.thl.termed.dao.jdbc.JdbcResourceDao;
import fi.thl.termed.dao.jdbc.JdbcResourceReferenceAttributeValueDao;
import fi.thl.termed.dao.jdbc.JdbcResourceTextAttributeValueDao;
import fi.thl.termed.dao.jdbc.JdbcSchemeDao;
import fi.thl.termed.dao.jdbc.JdbcSchemePermissionsDao;
import fi.thl.termed.dao.jdbc.JdbcSchemePropertyValueDao;
import fi.thl.termed.dao.jdbc.JdbcSchemeRoleDao;
import fi.thl.termed.dao.jdbc.JdbcTextAttributeDao;
import fi.thl.termed.dao.jdbc.JdbcTextAttributePermissionsDao;
import fi.thl.termed.dao.jdbc.JdbcTextAttributePropertyValueDao;
import fi.thl.termed.dao.jdbc.JdbcUserDao;
import fi.thl.termed.dao.jdbc.JdbcUserSchemeRoleDao;
import fi.thl.termed.domain.AppRole;
import fi.thl.termed.domain.Class;
import fi.thl.termed.domain.ClassId;
import fi.thl.termed.domain.JsTree;
import fi.thl.termed.domain.ObjectRolePermission;
import fi.thl.termed.domain.Permission;
import fi.thl.termed.domain.Property;
import fi.thl.termed.domain.PropertyValueId;
import fi.thl.termed.domain.ReferenceAttribute;
import fi.thl.termed.domain.ReferenceAttributeId;
import fi.thl.termed.domain.Resource;
import fi.thl.termed.domain.ResourceAttributeValueId;
import fi.thl.termed.domain.ResourceId;
import fi.thl.termed.domain.Scheme;
import fi.thl.termed.domain.SchemeRole;
import fi.thl.termed.domain.TextAttribute;
import fi.thl.termed.domain.TextAttributeId;
import fi.thl.termed.domain.User;
import fi.thl.termed.domain.UserSchemeRoleId;
import fi.thl.termed.exchange.Exchange;
import fi.thl.termed.exchange.Exporter;
import fi.thl.termed.exchange.rdf.ResourceRdfExchange;
import fi.thl.termed.exchange.table.ResourceTableExchange;
import fi.thl.termed.exchange.tree.ResourceContextJsTreeExporter;
import fi.thl.termed.exchange.tree.ResourceTreeExporter;
import fi.thl.termed.index.Index;
import fi.thl.termed.index.lucene.LuceneIndex;
import fi.thl.termed.index.lucene.ResourceDocumentConverter;
import fi.thl.termed.permission.PermissionEvaluator;
import fi.thl.termed.permission.common.AppRolePermissionEvaluator;
import fi.thl.termed.permission.common.ConjunctionPermissionEvaluator;
import fi.thl.termed.permission.common.DaoBasedObjectPermissionEvaluator;
import fi.thl.termed.permission.common.DisjunctionPermissionEvaluator;
import fi.thl.termed.permission.common.PermitAllPermissionEvaluator;
import fi.thl.termed.permission.resource.ClassIdBasedResourcePermissionEvaluator;
import fi.thl.termed.permission.resource.DelegatingResourcePermissionEvaluator;
import fi.thl.termed.permission.resource.ResourceReferenceAttributeValuePermissionEvaluator;
import fi.thl.termed.permission.resource.ResourceTextAttributeValuePermissionEvaluator;
import fi.thl.termed.permission.resource.SchemeIdBasedResourcePermissionEvaluator;
import fi.thl.termed.repository.Repository;
import fi.thl.termed.repository.impl.AbstractRepository;
import fi.thl.termed.repository.impl.ClassRepositoryImpl;
import fi.thl.termed.repository.impl.PropertyRepositoryImpl;
import fi.thl.termed.repository.impl.ReferenceAttributeRepositoryImpl;
import fi.thl.termed.repository.impl.ResourceRepositoryImpl;
import fi.thl.termed.repository.impl.SchemeRepositoryImpl;
import fi.thl.termed.repository.impl.TextAttributeRepositoryImpl;
import fi.thl.termed.repository.impl.UserRepositoryImpl;
import fi.thl.termed.service.Service;
import fi.thl.termed.service.common.LoggingService;
import fi.thl.termed.service.common.PermissionEvaluatingService;
import fi.thl.termed.service.common.RepositoryService;
import fi.thl.termed.service.common.TransactionalService;
import fi.thl.termed.service.resource.AttributeResolvingResourceService;
import fi.thl.termed.service.resource.AuditingResourceService;
import fi.thl.termed.service.resource.IdResolvingResourceService;
import fi.thl.termed.service.resource.IndexingResourceService;
import fi.thl.termed.service.resource.SchemeIdResolvingResourceService;
import fi.thl.termed.service.scheme.IndexingSchemeService;
import fi.thl.termed.service.scheme.ResolvingSchemeService;
import fi.thl.termed.service.scheme.ValidatingSchemeService;
import fi.thl.termed.spesification.Specification;
import fi.thl.termed.util.DateTypeAdapter;
import fi.thl.termed.util.LangValue;
import fi.thl.termed.util.MultimapTypeAdapterFactory;
import fi.thl.termed.util.StrictLangValue;
import fi.thl.termed.util.rdf.RdfModel;
import fi.thl.termed.web.PropertyController;
import fi.thl.termed.web.ResourceContextJsTreeController;
import fi.thl.termed.web.ResourceControllerSpringImpl;
import fi.thl.termed.web.ResourceRdfController;
import fi.thl.termed.web.ResourceTableController;
import fi.thl.termed.web.ResourceTreeController;
import fi.thl.termed.web.SchemeController;
import fi.thl.termed.web.UserController;

@Configuration
public class ApplicationBeans {

  // Utils

  @Bean
  public Gson gson() {
    return new GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(Date.class, new DateTypeAdapter().nullSafe())
        .registerTypeAdapterFactory(new MultimapTypeAdapterFactory())
        .create();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Controllers

  @Bean
  public ResourceControllerSpringImpl resourceController(
      Service<ResourceId, Resource> resourceService,
      Dao<UUID, Scheme> schemeDao,
      Dao<TextAttributeId, TextAttribute> textAttributeDao,
      PermissionEvaluator<UUID> schemePermissionEvaluator,
      PermissionEvaluator<ClassId> classPermissionEvaluator,
      PermissionEvaluator<TextAttributeId> textAttributeEvaluator) {
    return new ResourceControllerSpringImpl(resourceService,
                                            schemeDao,
                                            textAttributeDao,
                                            schemePermissionEvaluator,
                                            classPermissionEvaluator,
                                            textAttributeEvaluator);
  }

  @Bean
  public SchemeController schemeController(Service<UUID, Scheme> schemeService) {
    return new SchemeController(schemeService);
  }

  @Bean
  public ResourceTreeController resourceTreeController(
      Exporter<ResourceId, Resource, List<Resource>> resourceTreeExporter) {
    return new ResourceTreeController(resourceTreeExporter);
  }

  @Bean
  public ResourceContextJsTreeController resourceContextJsTreeController(
      Exporter<ResourceId, Resource, List<JsTree>> resourceContextJsTreeExporter) {
    return new ResourceContextJsTreeController(resourceContextJsTreeExporter);
  }

  @Bean
  public ResourceTableController resourceTableController(
      Exchange<ResourceId, Resource, List<String[]>> resourceTableExchange) {
    return new ResourceTableController(resourceTableExchange);
  }

  @Bean
  public ResourceRdfController resourceRdfController(
      Exchange<ResourceId, Resource, RdfModel> resourceRdfExchange) {
    return new ResourceRdfController(resourceRdfExchange);
  }

  @Bean
  public PropertyController propertyController(Service<String, Property> propertyService) {
    return new PropertyController(propertyService);
  }

  @Bean
  public UserController userController(Service<String, User> userService,
                                       PasswordEncoder passwordEncoder) {
    return new UserController(userService, passwordEncoder);
  }

  // Exporters

  @Bean
  public Exchange<ResourceId, Resource, List<String[]>> resourceTableExchange(
      Service<ResourceId, Resource> resourceService,
      Gson gson) {
    return new ResourceTableExchange(resourceService, gson);
  }

  @Bean
  public Exchange<ResourceId, Resource, RdfModel> resourceRdfExchange(
      Service<ResourceId, Resource> resourceService,
      Service<UUID, Scheme> schemeService) {
    return new ResourceRdfExchange(resourceService, schemeService);
  }

  @Bean
  public Exporter<ResourceId, Resource, List<JsTree>> resourceContextJsTreeExporter(
      Service<ResourceId, Resource> resourceService,
      Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao) {
    return new ResourceContextJsTreeExporter(resourceService, referenceAttributeDao);
  }

  @Bean
  public Exporter<ResourceId, Resource, List<Resource>> resourceTreeExporter(
      Service<ResourceId, Resource> resourceService,
      Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao) {
    return new ResourceTreeExporter(resourceService, referenceAttributeDao);
  }

  // Services

  @Bean
  public Service<String, User> userService(Repository<String, User> userRepository,
                                           PlatformTransactionManager transactionManager) {

    Service<String, User> service = new RepositoryService<String, User>(userRepository);

    service = new TransactionalService<String, User>(service, transactionManager);

    ImmutableMultimap<AppRole, Permission> appRolePermissions =
        ImmutableMultimap.<AppRole, Permission>builder()
            .putAll(AppRole.SUPERUSER, Permission.values()).build();

    service = new PermissionEvaluatingService<String, User>(
        service,
        new AppRolePermissionEvaluator<String>(appRolePermissions),
        new AppRolePermissionEvaluator<User>(appRolePermissions),
        new AppRolePermissionEvaluator<Specification<String, User>>(appRolePermissions));

    return service;
  }

  @Bean
  public Service<String, Property> propertyService(
      Repository<String, Property> propertyRepository,
      PlatformTransactionManager transactionManager) {

    Service<String, Property> service = new RepositoryService<String, Property>(propertyRepository);

    service = new TransactionalService<String, Property>(service, transactionManager);

    ImmutableMultimap<AppRole, Permission> appRolePermissions =
        ImmutableMultimap.<AppRole, Permission>builder()
            .putAll(AppRole.USER, Permission.READ)
            .putAll(AppRole.ADMIN, Permission.READ)
            .putAll(AppRole.SUPERUSER, Permission.values()).build();

    service = new PermissionEvaluatingService<String, Property>(
        service,
        new AppRolePermissionEvaluator<String>(appRolePermissions),
        new AppRolePermissionEvaluator<Property>(appRolePermissions),
        new AppRolePermissionEvaluator<Specification<String, Property>>(appRolePermissions));

    return service;
  }

  @Bean
  public Service<UUID, Scheme> schemeService(
      Repository<UUID, Scheme> schemeRepository,
      Repository<ResourceId, Resource> resourceRepository,
      Index<ResourceId, Resource> resourceIndex,
      Dao<UUID, Scheme> schemeDao,
      Dao<ResourceId, Resource> resourceDao,
      PlatformTransactionManager transactionManager) {

    Service<UUID, Scheme> service = new RepositoryService<UUID, Scheme>(schemeRepository);

    service = new TransactionalService<UUID, Scheme>(service, transactionManager);
    service = new LoggingService<UUID, Scheme>(service, Scheme.class);
    service = new IndexingSchemeService(service, resourceRepository, resourceIndex, resourceDao);
    service = new ValidatingSchemeService(service);
    service = new ResolvingSchemeService(service, schemeDao);

    ImmutableMultimap<AppRole, Permission> appRolePermissions =
        ImmutableMultimap.<AppRole, Permission>builder()
            .putAll(AppRole.USER, Permission.READ)
            .putAll(AppRole.ADMIN, Permission.values())
            .putAll(AppRole.SUPERUSER, Permission.values()).build();

    service = new PermissionEvaluatingService<UUID, Scheme>(
        service,
        new AppRolePermissionEvaluator<UUID>(appRolePermissions),
        new AppRolePermissionEvaluator<Scheme>(appRolePermissions),
        new AppRolePermissionEvaluator<Specification<UUID, Scheme>>(appRolePermissions));

    return service;
  }

  @Bean
  public Service<ResourceId, Resource> resourceService(
      Repository<ResourceId, Resource> resourceRepository,
      Index<ResourceId, Resource> resourceIndex,
      PermissionEvaluator<ResourceId> resourceIdPermissionEvaluator,
      PermissionEvaluator<Resource> resourcePermissionEvaluator,
      PermissionEvaluator<Specification<ResourceId, Resource>> resourceSpecificationPermissionEvaluator,
      PermissionEvaluator<ResourceAttributeValueId> textAttributeValueIdPermissionEvaluator,
      PermissionEvaluator<ResourceAttributeValueId> referenceAttributeValueIdPermissionEvaluator,
      Dao<UUID, Scheme> schemeDao,
      Dao<TextAttributeId, TextAttribute> textAttributeDao,
      Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao,
      Dao<ResourceId, Resource> resourceDao,
      Dao<ResourceAttributeValueId, StrictLangValue> textAttributeValueDao,
      Dao<ResourceAttributeValueId, ResourceId> referenceAttributeValueDao,
      PlatformTransactionManager transactionManager) {

    Service<ResourceId, Resource> service =
        new RepositoryService<ResourceId, Resource>(resourceRepository);

    service = new TransactionalService<ResourceId, Resource>(service, transactionManager);
    service = new IndexingResourceService(
        service, resourceRepository, resourceIndex, referenceAttributeValueDao);
    service = new AuditingResourceService(service, resourceDao);

//    service = new ReferenceAttributeValuePermissionEvaluatingService(
//        service, referenceAttributeValueDao, textAttributeValueIdPermissionEvaluator);
//    service = new TextAttributeValuePermissionEvaluatingService(
//        service, textAttributeValueDao, referenceAttributeValueIdPermissionEvaluator);
//    service = new ResourcePermissionEvaluatingService(
//        service, resourceIdPermissionEvaluator, resourcePermissionEvaluator,
//        resourceSpecificationPermissionEvaluator, resourceDao);

    service = new LoggingService<ResourceId, Resource>(service, Resource.class);

    service = new AttributeResolvingResourceService(
        service, textAttributeDao, referenceAttributeDao, resourceDao);
    service = new IdResolvingResourceService(service, resourceDao);
    service = new SchemeIdResolvingResourceService(service, schemeDao);

    return service;
  }

  // Permission evaluators

  @Bean
  public PermissionEvaluator<ResourceAttributeValueId> textAttributeValueIdPermissionEvaluator(
      PermissionEvaluator<TextAttributeId> textAttributeIdPermissionEvaluator) {
    return new ResourceTextAttributeValuePermissionEvaluator(
        textAttributeIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<ResourceAttributeValueId> referenceAttributeValueIdPermissionEvaluator(
      PermissionEvaluator<ReferenceAttributeId> referenceAttributeIdPermissionEvaluator) {
    return new ResourceReferenceAttributeValuePermissionEvaluator(
        referenceAttributeIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<Specification<ResourceId, Resource>> resourceSpecificationPermissionEvaluator() {
    return new PermitAllPermissionEvaluator<Specification<ResourceId, Resource>>();
  }

  @Bean
  public PermissionEvaluator<Resource> resourcePermissionEvaluator(
      PermissionEvaluator<ResourceId> resourceIdPermissionEvaluator) {
    return new DelegatingResourcePermissionEvaluator(resourceIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<ResourceId> resourceIdPermissionEvaluator(
      PermissionEvaluator<UUID> schemeIdPermissionEvaluator,
      PermissionEvaluator<ClassId> classIdPermissionEvaluator) {
    return new ConjunctionPermissionEvaluator<ResourceId>(
        new SchemeIdBasedResourcePermissionEvaluator(schemeIdPermissionEvaluator),
        new ClassIdBasedResourcePermissionEvaluator(classIdPermissionEvaluator));
  }

  @Bean
  public PermissionEvaluator<UUID> schemeIdPermissionEvaluator(
      Dao<ObjectRolePermission<UUID>, Void> schemePermissionDao) {

    PermissionEvaluator<UUID> schemeIdPermissionEvaluator =
        new DaoBasedObjectPermissionEvaluator<UUID>(schemePermissionDao);

    PermissionEvaluator<UUID> appRolePermissionEvaluator =
        new AppRolePermissionEvaluator<UUID>(
            ImmutableMultimap.<AppRole, Permission>builder()
                .putAll(AppRole.ADMIN, Permission.values())
                .putAll(AppRole.SUPERUSER, Permission.values()).build());

    return new DisjunctionPermissionEvaluator<UUID>(
        appRolePermissionEvaluator, schemeIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<ClassId> classIdPermissionEvaluator(
      Dao<ObjectRolePermission<ClassId>, Void> classPermissionDao) {

    PermissionEvaluator<ClassId> classIdPermissionEvaluator =
        new DaoBasedObjectPermissionEvaluator<ClassId>(classPermissionDao);

    PermissionEvaluator<ClassId> appRolePermissionEvaluator =
        new AppRolePermissionEvaluator<ClassId>(
            ImmutableMultimap.<AppRole, Permission>builder()
                .putAll(AppRole.ADMIN, Permission.values())
                .putAll(AppRole.SUPERUSER, Permission.values()).build());

    return new DisjunctionPermissionEvaluator<ClassId>(
        appRolePermissionEvaluator, classIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<TextAttributeId> textAttributeIdPermissionEvaluator(
      Dao<ObjectRolePermission<TextAttributeId>, Void> textAttributePermissionDao) {

    PermissionEvaluator<TextAttributeId> textAttributeIdPermissionEvaluator =
        new DaoBasedObjectPermissionEvaluator<TextAttributeId>(textAttributePermissionDao);

    PermissionEvaluator<TextAttributeId> appRolePermissionEvaluator =
        new AppRolePermissionEvaluator<TextAttributeId>(
            ImmutableMultimap.<AppRole, Permission>builder()
                .putAll(AppRole.ADMIN, Permission.values())
                .putAll(AppRole.SUPERUSER, Permission.values()).build());

    return new DisjunctionPermissionEvaluator<TextAttributeId>(
        appRolePermissionEvaluator, textAttributeIdPermissionEvaluator);
  }

  @Bean
  public PermissionEvaluator<ReferenceAttributeId> referenceAttributeIdPermissionEvaluator(
      Dao<ObjectRolePermission<ReferenceAttributeId>, Void> referenceAttributePermissionDao) {

    PermissionEvaluator<ReferenceAttributeId> textAttributeIdPermissionEvaluator =
        new DaoBasedObjectPermissionEvaluator<ReferenceAttributeId>(
            referenceAttributePermissionDao);

    PermissionEvaluator<ReferenceAttributeId> appRolePermissionEvaluator =
        new AppRolePermissionEvaluator<ReferenceAttributeId>(
            ImmutableMultimap.<AppRole, Permission>builder()
                .putAll(AppRole.ADMIN, Permission.values())
                .putAll(AppRole.SUPERUSER, Permission.values()).build());

    return new DisjunctionPermissionEvaluator<ReferenceAttributeId>(
        appRolePermissionEvaluator, textAttributeIdPermissionEvaluator);
  }

  // Indices

  @Bean
  public Index<ResourceId, Resource> resourceIndex(
      @Value("${fi.thl.termed.index:}") String indexPath, Gson gson) {
    return new LuceneIndex<ResourceId, Resource>(indexPath, new ResourceDocumentConverter(gson));
  }

  // Repositories

  @Bean
  public Repository<String, User> userRepository(
      Dao<String, User> userDao,
      Dao<UserSchemeRoleId, Void> userSchemeRoleDao) {
    return new UserRepositoryImpl(userDao, userSchemeRoleDao);
  }

  @Bean
  public Repository<String, Property> propertyRepository(
      Dao<String, Property> propertyDao,
      Dao<PropertyValueId<String>, LangValue> propertyPropertyValueDao) {
    return new PropertyRepositoryImpl(propertyDao,
                                      propertyPropertyValueDao);
  }

  @Bean
  public Repository<UUID, Scheme> schemeRepository(
      Dao<UUID, Scheme> schemeDao,
      Dao<SchemeRole, Void> schemeRoleDao,
      Dao<ObjectRolePermission<UUID>, Void> schemePermissionDao,
      Dao<PropertyValueId<UUID>, LangValue> schemePropertyValueDao,
      AbstractRepository<ClassId, Class> classRepository) {
    return new SchemeRepositoryImpl(schemeDao,
                                    schemeRoleDao,
                                    schemePermissionDao,
                                    schemePropertyValueDao,
                                    classRepository);
  }

  @Bean
  public AbstractRepository<ClassId, Class> classRepository(
      Dao<ClassId, Class> classDao,
      Dao<ObjectRolePermission<ClassId>, Void> classPermissionDao,
      Dao<PropertyValueId<ClassId>, LangValue> classPropertyValueDao,
      AbstractRepository<TextAttributeId, TextAttribute> textAttributeRepository,
      AbstractRepository<ReferenceAttributeId, ReferenceAttribute> referenceAttributeRepository) {
    return new ClassRepositoryImpl(classDao,
                                   classPermissionDao,
                                   classPropertyValueDao,
                                   textAttributeRepository,
                                   referenceAttributeRepository);
  }

  @Bean
  public AbstractRepository<TextAttributeId, TextAttribute> textAttributeRepository(
      Dao<TextAttributeId, TextAttribute> textAttributeDao,
      Dao<ObjectRolePermission<TextAttributeId>, Void> textAttributePermissionDao,
      Dao<PropertyValueId<TextAttributeId>, LangValue> textAttributePropertyValueDao) {
    return new TextAttributeRepositoryImpl(textAttributeDao,
                                           textAttributePermissionDao,
                                           textAttributePropertyValueDao);
  }

  @Bean
  public AbstractRepository<ReferenceAttributeId, ReferenceAttribute> referenceAttributeRepository(
      Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao,
      Dao<ObjectRolePermission<ReferenceAttributeId>, Void> referenceAttributePermissionDao,
      Dao<PropertyValueId<ReferenceAttributeId>, LangValue> referenceAttributePropertyValueDao) {
    return new ReferenceAttributeRepositoryImpl(referenceAttributeDao,
                                                referenceAttributePermissionDao,
                                                referenceAttributePropertyValueDao);
  }

  @Bean
  public Repository<ResourceId, Resource> resourceRepository(
      Dao<ResourceId, Resource> resourceDao,
      Dao<ResourceAttributeValueId, StrictLangValue> textAttributeValueDao,
      Dao<ResourceAttributeValueId, ResourceId> referenceAttributeValueDao,
      Dao<UUID, Scheme> schemeDao,
      Dao<PropertyValueId<UUID>, LangValue> schemePropertyValueDao,
      Dao<ClassId, Class> classDao,
      Dao<PropertyValueId<ClassId>, LangValue> classPropertyValueDao) {
    return new ResourceRepositoryImpl(resourceDao,
                                      textAttributeValueDao,
                                      referenceAttributeValueDao,
                                      schemeDao,
                                      schemePropertyValueDao,
                                      classDao,
                                      classPropertyValueDao);
  }

  // DAOs

  @Bean
  public Dao<String, User> userDao(DataSource dataSource) {
    return new JdbcUserDao(dataSource);
  }

  @Bean
  public Dao<UserSchemeRoleId, Void> userSchemeRoleDao(DataSource dataSource) {
    return new JdbcUserSchemeRoleDao(dataSource);
  }

  @Bean
  public Dao<String, Property> propertyDao(DataSource dataSource) {
    return new JdbcPropertyDao(dataSource);
  }

  @Bean
  public Dao<PropertyValueId<String>, LangValue> propertyPropertyValueDao(DataSource dataSource) {
    return new JdbcPropertyPropertyValueDao(dataSource);
  }

  @Bean
  public Dao<UUID, Scheme> schemeDao(DataSource dataSource) {
    return new JdbcSchemeDao(dataSource);
  }

  @Bean
  public Dao<SchemeRole, Void> schemeRoleDao(DataSource dataSource) {
    return new JdbcSchemeRoleDao(dataSource);
  }

  @Bean
  public Dao<ObjectRolePermission<UUID>, Void> schemePermissionDao(DataSource dataSource) {
    return new JdbcSchemePermissionsDao(dataSource);
  }

  @Bean
  public Dao<PropertyValueId<UUID>, LangValue> schemePropertyValueDao(DataSource dataSource) {
    return new JdbcSchemePropertyValueDao(dataSource);
  }


  @Bean
  public Dao<ClassId, Class> classDao(DataSource dataSource) {
    return new JdbcClassDao(dataSource);
  }

  @Bean
  public Dao<ObjectRolePermission<ClassId>, Void> classPermissionDao(DataSource dataSource) {
    return new JdbcClassPermissionsDao(dataSource);
  }

  @Bean
  public Dao<PropertyValueId<ClassId>, LangValue> classPropertyValueDao(DataSource dataSource) {
    return new JdbcClassPropertyValueDao(dataSource);
  }

  @Bean
  public Dao<ReferenceAttributeId, ReferenceAttribute> referenceAttributeDao(
      DataSource dataSource) {
    return new JdbcReferenceAttributeDao(dataSource);
  }

  @Bean
  public Dao<ObjectRolePermission<ReferenceAttributeId>, Void> referenceAttributePermissionDao(
      DataSource dataSource) {
    return new JdbcReferenceAttributePermissionsDao(dataSource);
  }

  @Bean
  public Dao<PropertyValueId<ReferenceAttributeId>, LangValue> referenceAttributePropertyValueDao(
      DataSource dataSource) {
    return new JdbcReferenceAttributePropertyValueDao(dataSource);
  }

  @Bean
  public Dao<TextAttributeId, TextAttribute> textAttributeDao(DataSource dataSource) {
    return new JdbcTextAttributeDao(dataSource);
  }

  @Bean
  public Dao<ObjectRolePermission<TextAttributeId>, Void> textAttributePermissionDao(
      DataSource dataSource) {
    return new JdbcTextAttributePermissionsDao(dataSource);
  }

  @Bean
  public Dao<PropertyValueId<TextAttributeId>, LangValue> textAttributePropertyValueDao(
      DataSource dataSource) {
    return new JdbcTextAttributePropertyValueDao(dataSource);
  }

  @Bean
  public Dao<ResourceId, Resource> resourceDao(DataSource dataSource) {
    return new JdbcResourceDao(dataSource);
  }

  @Bean
  public Dao<ResourceAttributeValueId, ResourceId> resourceReferenceAttributeValueDao(
      DataSource dataSource) {
    return new JdbcResourceReferenceAttributeValueDao(dataSource);
  }

  @Bean
  public Dao<ResourceAttributeValueId, StrictLangValue> resourceTextAttributeValueDao(
      DataSource dataSource) {
    return new JdbcResourceTextAttributeValueDao(dataSource);
  }

}
