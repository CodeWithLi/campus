package campus.ai.rag.service.mapper.model;

import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiTokenBillingLogMapper extends BaseMapper<AiTokenBillingLogEntity> {

    @Select("""
            <script>
            SELECT
              ${bucketExpr} AS bucket,
              COUNT(1) AS callCount,
              ROUND(IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(1), 0), 0), 2) AS successRate,
              ROUND(IFNULL(AVG(CASE WHEN elapsed_ms IS NULL THEN NULL ELSE elapsed_ms END), 0), 0) AS avgElapsedMs
            FROM ai_token_billing_log
            <where>
              <if test="startTime != null"> AND created_at <![CDATA[>=]]> #{startTime} </if>
              <if test="endTime != null"> AND created_at <![CDATA[<=]]> #{endTime} </if>
            </where>
            GROUP BY bucket
            ORDER BY bucket ASC
            </script>
            """)
    List<Map<String, Object>> selectCallTrend(@Param("bucketExpr") String bucketExpr,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
              ${bucketExpr} AS bucket,
              IFNULL(SUM(prompt_tokens), 0) AS promptTokens,
              IFNULL(SUM(completion_tokens), 0) AS completionTokens,
              IFNULL(SUM(total_tokens), 0) AS totalTokens,
              IFNULL(SUM(consumed_points), 0) AS consumedPoints
            FROM ai_token_billing_log
            <where>
              status = 1
              <if test="startTime != null"> AND created_at <![CDATA[>=]]> #{startTime} </if>
              <if test="endTime != null"> AND created_at <![CDATA[<=]]> #{endTime} </if>
            </where>
            GROUP BY bucket
            ORDER BY bucket ASC
            </script>
            """)
    List<Map<String, Object>> selectTokenConsumption(@Param("bucketExpr") String bucketExpr,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
              model_code AS modelCode,
              COUNT(1) AS callCount
            FROM ai_token_billing_log
            <where>
              <if test="startTime != null"> AND created_at <![CDATA[>=]]> #{startTime} </if>
              <if test="endTime != null"> AND created_at <![CDATA[<=]]> #{endTime} </if>
            </where>
            GROUP BY model_code
            ORDER BY callCount DESC
            </script>
            """)
    List<Map<String, Object>> selectModelUsageShare(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}

