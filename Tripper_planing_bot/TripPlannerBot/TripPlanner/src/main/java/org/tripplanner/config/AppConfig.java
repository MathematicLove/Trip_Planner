package org.tripplanner.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.modulith.Modulithic;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan("org.tripplanner")
@EnableJpaRepositories("org.tripplanner.repository.jpa")
@EnableReactiveMongoRepositories("org.tripplanner.repository.mongo")
@EnableKafka
@EnableScheduling
@EnableTransactionManagement
@EnableWebMvc                 // MVC без Boot
@Modulithic
public class AppConfig {

    private final Environment env;
    public AppConfig(Environment env) { this.env = env; }

    /* ---------- JDBC ------------------------------------------------------ */

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}")      String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String pass) {

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        return new HikariDataSource(cfg);
    }

    /* ---------- JPA ------------------------------------------------------- */

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
        var f = new LocalContainerEntityManagerFactoryBean();
        f.setDataSource(ds);
        f.setPackagesToScan("org.tripplanner.domain");

        var adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        adapter.setShowSql(env.getProperty("spring.jpa.show-sql", Boolean.class, false));
        f.setJpaVendorAdapter(adapter);

        var p = new Properties();
        p.put("hibernate.hbm2ddl.auto",
              env.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        p.put("hibernate.dialect",
              env.getProperty("spring.jpa.properties.hibernate.dialect"));
        f.setJpaProperties(p);
        return f;
    }

    @Bean public PlatformTransactionManager
    transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /* ---------- Mongo (reactive) ----------------------------------------- */

    @Bean
    public MongoClient reactiveMongoClient(
            @Value("${spring.data.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(
            MongoClient c,
            @Value("${spring.data.mongodb.database}") String db) {
        return new ReactiveMongoTemplate(c, db);
    }

    /* ---------- Kafka ----------------------------------------------------- */

    @Bean
    public KafkaAdmin kafkaAdmin(
            @Value("${spring.kafka.bootstrap-servers}") String brokers) {
        return new KafkaAdmin(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers));
    }

    @Bean
    public ProducerFactory<String,String> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String brokers) {

        var cfg = new HashMap<String,Object>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean public KafkaTemplate<String,String>
    kafkaTemplate(ProducerFactory<String,String> pf) { return new KafkaTemplate<>(pf); }

    @Bean
    public ConsumerFactory<String,String> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String brokers) {

        var cfg = new HashMap<String,Object>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "trip-planner-group");
        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,String>
    kafkaListenerContainerFactory(ConsumerFactory<String,String> cf) {
        var f = new ConcurrentKafkaListenerContainerFactory<String,String>();
        f.setConsumerFactory(cf);
        return f;
    }

    /* ---------- MVC view-resolver (JSP из /ui/) --------------------------- */

    @Bean
    public InternalResourceViewResolver viewResolver() {
        var vr = new InternalResourceViewResolver();
        vr.setPrefix("/ui/");   // внутри classpath
        vr.setSuffix(".jsp");
        return vr;
    }

    /* ---------- WebClient ------------------------------------------------- */

    @Bean public WebClient webClient() { return WebClient.create(); }

    /* ----------- UI -------------------------------------------------------*/
    @Bean
    public InternalResourceViewResolver jspViewResolver() {
        var vr = new InternalResourceViewResolver();
        vr.setPrefix("/WEB-INF/ui/");    // каталог с JSP
        vr.setSuffix(".jsp");            // расширение
        vr.setViewClass(org.springframework.web.servlet.view.JstlView.class);
        return vr;
    }

}
