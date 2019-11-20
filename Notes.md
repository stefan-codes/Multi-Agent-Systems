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
import javax.servlet.http.HttpServletRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders..; // Delete, Get, Post

public class RestControllerTest extends BaseTestClass {

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
```

>NOTE: TRY TO TRANSLATE TO ANNOTATIONS


### Rest Controller Multiple URL Mappings

Prototype, Singleton viable

```java
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