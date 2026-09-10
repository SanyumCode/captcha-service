package net.Captcha.mongod;

import java.util.Date;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperators;
import net.Captcha.common.Image;
import net.Captcha.common.User;
import net.Captcha.config.AppConfig;
/*
 * 记住区分Fliter里面到底是String还是int/long类型，不然查询不到
 */
public class DBManager {
    private static final Logger logger = LoggerFactory.getLogger(DBManager.class);
      private static final DBManager INSTANCE = new DBManager();
      private static final AppConfig CONFIG = AppConfig.current();

      // MongoDB连接池配置参数
      private static final int MAX_POOL_SIZE = 50;           // 最大连接池大小（默认100，降低以避免资源耗尽）
      private static final int MIN_POOL_SIZE = 5;            // 最小连接池大小（保持一定连接数）
      private static final int MAX_CONNECTION_IDLE_TIME_MS = 60000;  // 连接空闲超时60秒
      private static final int MAX_CONNECTION_LIFE_TIME_MS = 3600000; // 连接最大生命周期1小时
      private static final int CONNECT_TIMEOUT_MS = 10000;   // 连接超时10秒
      private static final int SOCKET_TIMEOUT_MS = 30000;     // Socket超时30秒
      private static final int SERVER_SELECTION_TIMEOUT_MS = 10000; // 服务器选择超时10秒

      private final Datastore datastore;
      private final MongoClient client;
      private final MongoDatabase db;
      private final MongoCollection<Document> counters;



      private DBManager() {
          try {
              // 使用ConnectionString和MongoClientSettings进行更精细的配置
              ConnectionString connectionString = new ConnectionString(CONFIG.mongoUri());
              MongoClientSettings settings = MongoClientSettings.builder()
                      .applyConnectionString(connectionString)
                      .applyToConnectionPoolSettings(builder -> builder
                              .maxSize(MAX_POOL_SIZE)
                              .minSize(MIN_POOL_SIZE)
                              .maxConnectionIdleTime(MAX_CONNECTION_IDLE_TIME_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                              .maxConnectionLifeTime(MAX_CONNECTION_LIFE_TIME_MS, java.util.concurrent.TimeUnit.MILLISECONDS))
                      .applyToSocketSettings(builder -> builder
                              .connectTimeout(CONNECT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                              .readTimeout(SOCKET_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS))
                      .applyToServerSettings(builder -> builder
                              .heartbeatFrequency(10000, java.util.concurrent.TimeUnit.MILLISECONDS))
                      .applyToClusterSettings(builder -> builder
                              .serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS))
                      .build();

              client = MongoClients.create(settings);
              db = client.getDatabase(CONFIG.mongoDatabase());
              datastore = Morphia.createDatastore(client, CONFIG.mongoDatabase());
              this.counters = db.getCollection("counters");

              logger.info("MongoDB连接初始化成功 - 连接池配置: maxPoolSize={}, minPoolSize={}, " +
                      "maxIdleTime={}ms, maxLifeTime={}ms",
                      MAX_POOL_SIZE, MIN_POOL_SIZE, MAX_CONNECTION_IDLE_TIME_MS, MAX_CONNECTION_LIFE_TIME_MS);
          } catch (Exception e) {
              logger.error("MongoDB初始化失败: {}", e.getClass().getSimpleName());
              throw new RuntimeException("MongoDB初始化失败，请检查环境变量和网络连接");
          }
      }

      public static DBManager getInstance() {
         return INSTANCE;
      }

      /**
       * 检查MongoDB连接健康状态
       * @return true如果连接正常，false否则
       */
      public boolean isHealthy() {
          try {
              // 执行一个简单的ping操作来检查连接
              db.runCommand(new Document("ping", 1));
              return true;
          } catch (Exception e) {
              logger.warn("MongoDB健康检查失败", e);
              return false;
          }
      }

      /**
       * 获取连接池统计信息（用于诊断）
       */
      public void logConnectionPoolStats() {
          try {
              // MongoDB Java驱动不直接提供连接池统计API
              // 但我们可以通过执行命令来检查连接状态
              db.runCommand(new Document("ping", 1));
              logger.info("MongoDB服务器状态检查完成");

              // 记录连接池配置信息
              logger.info("MongoDB连接池配置 - maxPoolSize: {}, minPoolSize: {}, " +
                      "maxIdleTime: {}ms, maxLifeTime: {}ms",
                      MAX_POOL_SIZE, MIN_POOL_SIZE, MAX_CONNECTION_IDLE_TIME_MS, MAX_CONNECTION_LIFE_TIME_MS);
          } catch (Exception e) {
              logger.error("获取MongoDB连接池统计信息失败", e);
          }
      }

      public void close() {
          if (client != null) {
              try {
                  client.close();
                  logger.info("MongoDB连接已关闭");
              } catch (Exception e) {
                  logger.error("关闭MongoDB连接时出错", e);
              }
          }
      }

      public void addUser(String userName, String apiKey) {
          try {
              User user = new User();
              user.setUserName(userName);
              user.setApiKey(apiKey);
              long nextId = getNextSequence("users");
              user.setId((int) nextId);
              datastore.save(user);
              logger.info("User created with ID: {}", nextId);
          } catch (Exception e) {
              logger.error("添加用户失败: userName={}", userName, e);
              throw new RuntimeException("Failed to add user", e);
          }
      }
      public boolean addImage(Image img) {
          try {
              long nextId = getNextSequence("images");
              img.setId(nextId);
              datastore.save(img);
              return true;
          } catch (Exception e) {
              logger.error("添加图片失败: uid={}", img.getUID(), e);
              return false;
          }
      }
      public void delImage(Image img) {
          Query<Image> query = datastore.find(Image.class)
                  .filter(dev.morphia.query.filters.Filters.eq("_id", img.getId()));  // 使用 Filters.eq
          query.delete();
      }
      public Image getImage(int usid, String uid) {
          Query<Image> query = datastore.find(Image.class)
                  .filter(dev.morphia.query.filters.Filters.eq("usid", usid),
                            dev.morphia.query.filters.Filters.eq("uid", uid));
          return query.first();
      }
      public void updateImage(String message, String coded) {
        // 2. 构造查询条件 (两个字段)
          String[] split = message.split("-");
          // 数组越界防护
          if (split.length < 3) {
              logger.error("updateImage: message格式错误: {}", message);
              return;
          }
          try {
                Query<Image> query = datastore.find(Image.class)
                        .filter(dev.morphia.query.filters.Filters.eq("usid", Integer.valueOf(split[0])))
                        .filter(dev.morphia.query.filters.Filters.eq("uid", split[2]));
                UpdateOptions options = new UpdateOptions().multi(true);
                query.update(options,
                        UpdateOperators.set("cmt", new Date()),
                        UpdateOperators.set("status", "S"),
                        UpdateOperators.set("coded", coded));
          } catch (NumberFormatException e) {
              logger.error("updateImage: usid格式错误: {}", split[0], e);
          } catch (Exception e) {
              logger.error("updateImage: 更新失败: message={}", message, e);
          }
      }
      public void invalidImage(String message) {
          String[] split = message.split("-");
          // 数组越界防护
          if (split.length < 3) {
              logger.error("invalidImage: message格式错误: {}", message);
              return;
          }
          try {
              Query<Image> query = datastore.find(Image.class)
                        .filter(dev.morphia.query.filters.Filters.eq("usid", Integer.valueOf(split[0])))
                        .filter(dev.morphia.query.filters.Filters.eq("uid", split[2]));
                UpdateOptions options = new UpdateOptions().multi(true);
                query.update(options,
                        UpdateOperators.set("cmt", new Date()),
                        UpdateOperators.set("status", "F"));
          } catch (NumberFormatException e) {
              logger.error("invalidImage: usid格式错误: {}", split[0], e);
          } catch (Exception e) {
              logger.error("invalidImage: 更新失败: message={}", message, e);
          }
      }
      public User getUserByName(String userName) {
          Query<User> query = datastore.find(User.class)
                  .filter(dev.morphia.query.filters.Filters.eq("name", userName));  // 使用 Filters.eq
          return query.first();
      }
      public int checkUser(String userName, String apiK) {
          int usid = -1;
          User target = getUserByName(userName);
          if(target == null) return -1;
          String apiKey = target.getApiKey();
          // 性能优化：常量在前，避免NPE，使用equals比较
          if(apiKey != null && apiKey.equals(apiK)) {
              usid = target.getId();
          }
          return usid;
      }

      public boolean isUIDInUser(String uid, int usid) {
          // 性能优化：使用exists()而不是count()，只需要判断是否存在
          Query<Image> query = datastore.find(Image.class)
                  .filter(dev.morphia.query.filters.Filters.eq("usid", usid),
                          dev.morphia.query.filters.Filters.eq("uid", uid));
          // 使用limit(1)和first()代替count()，性能更好
          Image result = query.first();
          return result != null;
        }

      private long getNextSequence(String key) {
            Bson filter = Filters.eq("_id", key);
            Bson update = Updates.inc("seq", 1L);

            Document result = counters.findOneAndUpdate(
                    filter,
                    update,
                    new FindOneAndUpdateOptions()
                            .upsert(true)
                            .returnDocument(ReturnDocument.AFTER)
            );
            return result.getLong("seq");
        }
}
