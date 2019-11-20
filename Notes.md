# Multi-Agent Systems notes using JADE with Java
The JADE jar can be downloaded from:
https://jade.tilab.com/download/jade/

Documentation on JADE:
https://jade.tilab.com/doc/api/index.html

Recommended book:
Bellifemine, Fabio Luigi, et al. Developing Multi-Agent Systems with JADE. ProQuest, 2007.

## H2 Types of Agents

* **Remote Management Agent**

* *Remote Management Agent*

### H3 Heather

### Dependencies

The Tool uses a variety of artifacts to perform Unit and Integration testing, adapted to its legacy dependencies.

* **junit**
```XML
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>VERSION</version>
</dependency>
```

* **mockito**
```XML
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>VERSION</version>
</dependency
```

```xml
<dependency>
	<groupId>org.hsqldb</groupId>
	<artifactId>hsqldb</artifactId>
	<version>VERSION</version>
</dependency>

```


## Controller Layer

```java
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework*;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders..; // Delete, Get, Post

public class RestControllerTest extends BaseTestClass {

    @Mock
    private Service mockService;

    @InjectMocks
    private RestController restController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(restController).build();
    }

    @Test
    public void testBaseURL() throws Exception {
        this.mockMvc.perform(get("/URL"))
                .andExpect(status().is(200));
    }

    /**
     * GET Request Test
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetRequest() throws Exception {
        this.mockMvc.perform(get("/URL"))
                .andExpect(status().is(200))
                .andExpect(content().contentType(MediaType.valueOf("application/json;charset=UTF-8")));
    }

    /**
     * POST Request Test
     * ArgumentMatchers.anyString()
     * ArgumentMatchers.anyList()
     *
     * @throws Exception the exception
     */
    @Test
    public void testPostRequest() throws Exception {
        when(service.serviceMethod(ArgumentMatchers.anyType()))
                .thenReturn(RESPONSE_OBJECT);
        MvcResult result = this.mockMvc.perform(post("/URL")
                .content("{ REQUEST JSON CONTENT }")
                .with((MockHttpServletRequest request) -> {
                    request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    request.addHeader("authorization", "Basic " 
                        + Base64Utils.encodeToString("user:password".getBytes()));
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(EXPECTED, content);
    }

    /**
     * DELETE Request Test
     *
     * @throws Exception the exception
     */
    @Test
    public void testDeleteRequest() throws Exception {
        when(service.serviceMethod(isA(HttpServletRequest.class))).thenReturn(Boolean.FALSE);
        MvcResult result = this.mockMvc.perform(delete("/URL")
                .content("{ REQUEST JSON CONTENT }")
                .with((MockHttpServletRequest request) -> {
                    request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    request.addHeader("authorization", "Basic " + Base64Utils.encodeToString("user:password".getBytes()));
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertEquals(EXPECTED, content);
    }
}
```

## Service Layer

```java
public class ServiceTest extends BaseTestClass {

    @Mock
    private ClassDAO mockClassDAO;

    @InjectMocks
    private Service service;

    private Service mockedService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockedService = Mockito.spy(service);
    }

    /**
    ArgumentMatchers.anyList(), anyString()
    */
    @Test
    public void testExampleMethod() {
        when(mockedService.serviceMethod(ArgumentMatchers.anyType())).thenReturn(Collections.emptyList());
        doNothing().when(mockedService).insideMethod(ArgumentMatchers.anyList(), ArgumentMatchers.anyString());
        
        ...

        verify(mockedService, times(1)).serviceMethod();
        
        ...
        
        assertEquals(EXPECTED, ACTUAL);
    }
```

## DAO Layer

HSQLDB

https://stackoverflow.com/questions/11540758/hibernate-connection-with-hsqldb

```XML
<bean id="dataSource" class="org.hsqldb.jdbc.JDBCDataSource">
        <property name="database" value="jdbc:hsqldb:mem:dbname;sql.syntax_mss=true" />
        <property name="user" value="sa"/>
        <property name="password" value=""/>
</bean>

<bean name="schemaCreator" class="" init-method="init">
        <property name="dataSource" ref="dataSource"/>
        <property name="schemaNames">
            <list>
                <value>shemaName1</value>
                <value>shemaName2</value>
            </list>
        </property>
    </bean>

<bean id="sessionFactory" class="org.springframework.orm.hibernate3.LocalSessionFactoryBean" lazy-init="true" depends-on="schemaCreator, BEAN DEPENDENCIES">
        <property name="dataSource" ref="dataSource" />
        <property name="hibernateProperties">
            <props>
                <prop key="hibernate.dialect">org.hibernate.dialect.H2Dialect</prop>
                <prop key="hibernate.hbm2ddl.auto">create-drop</prop>
                <prop key="hibernate.cache.use_second_level_cache">false</prop>
                <prop key="hibernate.cache.use_query_cache">false</prop>
                <prop key="hibernate.show_sql">true</prop>
                <prop key="hibernate.use_sql_comments">true</prop>
            </props>
        </property>
        <property name="mappingResources" ref="mappingResources" />
        <property name="namingStrategy">
            <bean class="CUSTOM NAMING STRATEGY" />
        </property>
    </bean>

    <bean name="queryRunner" class="QueryRunner" init-method="init">
        <property name="dataSource" ref="dataSource"/>
        <property name="queries">
            <util:list>
                <value>drop table TABLE_NAME if exists</value>
                <value>create table TABLE_NAME (column_name char(25), column_name char(255))</value>
                <value>insert into TABLE_NAME values ('VALUE', 'VALUE')</value>
            </util:list>
        </property>
    </bean>

    <!-- Transaction Manager? -->

    <util:list id="mappingResources">
        <value>hibernate/MappingResource.hbm.xml</value>
    </util:list>
```

>NOTE: TRY TO TRANSLATE TO ANNOTATIONS


### Rest Controller Multiple URL Mappings

Prototype, Singleton viable

```java
@RestController
@RequestMapping(value = {
    "REQUEST_BASE_URL", 
    "REQUEST_SECOND_BASE_URL"
})
public class RestController {}
```

### DAO Layer implementation

We can authenticate the user and pass in the variable from Controller (Request) > Service > DAO

```java
public interface ClassDAO {
    @Transactional
	public void updateMethod(final String passedVariable);
}
```

```java
public class DAOImpleClass extends HibernateDaoSupport implements DAOClass {

public UserGroupDefDAOImpl() { super(); }


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED,
        rollbackFor = Throwable.class)
	public void updateMethod(final String passedVariable) {
		try {
			getHibernateTemplate().execute((final Session session) -> ) {
				// Handle Session

                MappedObj model = new MappedObj();

                // Handle model

				session.saveOrUpdate(model);
				session.flush();
				session.refresh(model);

				// Clear session context
				return 0;
			});
		} catch (Exception e) {
			throw new Exception("Failed to update " + e);
		}
	}
}
```

### Hibernate: mapping results to custom Pojo or entity
```java
return session.createSQLQuery(query)
	.addScalar("column_name", StringType.INSTANCE)
	.setParameterList("queryParameter", parameter)
	.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
	.list();
```

## Useful

* Hibernate Forumulas
* Spring Security
* Authentication Interceptor (Sprign)
* Authentication Service