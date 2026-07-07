package me.aloic.apeurival.util;

import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.osu.beatmap.BeatmapDTO;
import me.aloic.apeurival.entity.dto.osu.beatmap.ScoreLazerDTO;
import me.aloic.apeurival.entity.dto.osu.player.BeatmapUserScoreLazer;
import me.aloic.apeurival.entity.dto.osu.player.BeatmapUserScores;
import me.aloic.apeurival.entity.dto.osu.player.PlayerInfoDTO;
import me.aloic.apeurival.entity.dto.plus.*;
import me.aloic.apeurival.enums.HTTPTypeEnum;
import me.aloic.apeurival.enums.OsuMod;
import me.aloic.apeurival.enums.OsuMode;
import me.aloic.apeurival.enums.ScorePerformanceDimension;
import me.aloic.apeurival.exception.LazybotRuntimeException;
import me.aloic.apeurival.exception.NotFoundException;
import me.aloic.apeurival.monitor.TokenMonitor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataExtractor
{
    @Resource
    private ApiRequestExecutor apiRequestExecutor;

    /**
     * 根据用户名和模式获取用户信息
     * @param playerName 用户名
     * @param mode 模式字符
     * @return 玩家信息DTO对象
     */

    public PlayerInfoDTO extractPlayerInfoDTO(String playerName, String mode)
    {
        try{
            PlayerInfoDTO playerInfoDTO = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfPlayerInfo(playerName,mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    PlayerInfoDTO.class);
            if(playerInfoDTO.getId()==null) {
                throw new LazybotRuntimeException("没这B人: " + playerName);
            }
            return playerInfoDTO;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("没这B人: " + playerName);
        }
    }


    /**
     * 根据用户ID和模式获取用户信息
     * @param playerId 用户名
     * @param mode 模式字符
     * @return 玩家信息DTO对象
     */

    public PlayerInfoDTO extractPlayerInfoDTO(Integer playerId, String mode) {
       try{
           PlayerInfoDTO playerInfoDTO = apiRequestExecutor.execute(
                   URLBuildUtil.buildURLOfPlayerInfo(playerId,mode),
                   HTTPTypeEnum.GET,
                   TokenMonitor.getToken(),
                   null,
                   PlayerInfoDTO.class);
           if(playerInfoDTO.getId()==null) {
               throw new LazybotRuntimeException("没这B人: " + playerId);
           }
           return playerInfoDTO;
       }
       catch (NotFoundException e) {
           throw new LazybotRuntimeException("没这B人: " + playerId);
       }
    }


    /**
     * 根据用户ID获取PP+信息
     * @param playerId 用户ID
     * @return PP+玩家信息
     */
    public PPPlusPerformance extractPerformancePlusPlayerTotal(Integer playerId)
    {
        try{
            LazybotWebPlayerPerformance performance = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfPlayerPerformancePlus(playerId),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getLazybotToken(),
                    null,
                    LazybotWebPlayerPerformance.class);
            if(performance.getData()==null) {
                throw new LazybotRuntimeException("获取" + playerId + "用户pp+失败");
            }
            return performance.getData().getPerformances();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("获取" + playerId + "用户pp+失败");
        }
    }

    /**
     * 根据用户ID更新用户PP+数据
     * @param playerId 用户ID
     * @return 更新后PP+玩家信息
     */
    public PPPlusPerformance extractPerformancePlusPlayerUpdate(Integer playerId)
    {
        try{
            LazybotWebPlayerPerformance performance = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfUpdatePerformancePlus(playerId),
                    HTTPTypeEnum.POST,
                    TokenMonitor.getLazybotToken(),
                    null,
                    LazybotWebPlayerPerformance.class);
            if(performance.getData()==null) {
                throw new LazybotRuntimeException("更新" + playerId + "用户pp+失败，可能由于你的最近游玩为空");
            }
            return performance.getData().getPerformances();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("更新" + playerId + "用户pp+失败，可能由于你的最近游玩为空");
        }
    }

    /**
     * 重建玩家全部BP数据 (POST /player/reinit)
     * 拉取玩家osu! API的BP 200, 下载谱面文件并并行计算所有分数的pp+
     * @param playerId 用户ID
     * @return 重建后的PP+玩家信息
     */
    public PPPlusPerformance extractPerformancePlusPlayerReinit(Integer playerId)
    {
        try{
            LazybotWebPlayerPerformance performance = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfReinitPerformancePlus(playerId),
                    HTTPTypeEnum.POST,
                    TokenMonitor.getLazybotToken(),
                    null,
                    LazybotWebPlayerPerformance.class);
            if(performance.getData()==null) {
                throw new LazybotRuntimeException("重建" + playerId + "用户最佳成绩pp+数据失败");
            }
            return performance.getData().getPerformances();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("重建" + playerId + "用户最佳成绩pp+数据失败");
        }
    }

    /**
     * 根据用户ID和地图ID添加成绩到PP+服务器
     * @param playerId 用户ID
     * @param beatmapId 地图ID
     * @return 该地图ID的PP+详情
     */
    public LazybotScorePerformance extractPerformancePlusAddScore(Integer playerId, Integer beatmapId)
    {
        try{
            LazybotWebResult<LazybotScorePerformance> result = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfAddScorePerformancePlus(playerId,beatmapId),
                    HTTPTypeEnum.POST,
                    TokenMonitor.getLazybotToken(),
                    null,
                    new TypeReference<LazybotWebResult<LazybotScorePerformance>>() {});
            if(result.getData()==null) {
                throw new LazybotRuntimeException("添加用户" + playerId + "在" +beatmapId +"上的成绩失败");
            }
            return result.getData();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("添加用户" + playerId + "在" +beatmapId +"上的成绩失败");
        }
    }

    /**
     * 根据用户ID和和Plus的维度获取PP+成绩
     * @param playerId 用户ID
     * @param dimension 维度
     * @param offset 偏移量
     * @param limit 请求最大返回数量
     * @return 该玩家在该维度下的PP+列表
     */
    public List<ScorePerformanceDTO> extractPerformancePlusDimension(Integer playerId, ScorePerformanceDimension dimension, int offset, int limit)
    {
        try{
            LazybotWebResult<List<ScorePerformanceDTO>> result = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfScorePerformanceDimensionPlus(playerId,dimension,offset,limit),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getLazybotToken(),
                    null,
                    new TypeReference<LazybotWebResult<List<ScorePerformanceDTO>>>() {});
            if(result.getData()==null) {
                throw new LazybotRuntimeException("获取成绩为空");
            }
            return result.getData();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("获取成绩失败: " + e.getMessage());
        }
    }

    /**
     * 查询PP+服务器元数据统计 (GET /stats/count)
     */
    public StatsCount extractStatsCount()
    {
        LazybotWebResult<StatsCount> result = apiRequestExecutor.execute(
                URLBuildUtil.buildURLOfStatsCount(),
                HTTPTypeEnum.GET,
                null,
                null,
                new TypeReference<LazybotWebResult<StatsCount>>() {});
        if (result.getData() == null) {
            throw new LazybotRuntimeException("获取统计数据失败");
        }
        return result.getData();
    }

    /**
     * 查询PP+服务器API调用统计 (GET /stats/usage)
     */
    public Map<String, StatsApiUsage> extractStatsUsage()
    {
        LazybotWebResult<Map<String, StatsApiUsage>> result = apiRequestExecutor.execute(
                URLBuildUtil.buildURLOfStatsUsage(),
                HTTPTypeEnum.GET,
                null,
                null,
                new TypeReference<LazybotWebResult<Map<String, StatsApiUsage>>>() {});
        if (result.getData() == null) {
            throw new LazybotRuntimeException("获取API调用统计失败");
        }
        return result.getData();
    }

    /**
     * 查询上次批量更新玩家数 (GET /stats/player/updated)
     */
    public Integer extractStatsPlayerUpdated()
    {
        LazybotWebResult<Integer> result = apiRequestExecutor.execute(
                URLBuildUtil.buildURLOfStatsPlayerUpdated(),
                HTTPTypeEnum.GET,
                null,
                null,
                new TypeReference<LazybotWebResult<Integer>>() {});
        return result.getData();
    }

    /**
     * 获取用户的最近游玩成绩列表
     * @param playerId 用户ID
     * @param type 请求类型, 0会包含失败成绩
     * @param limit 请求最大返回数量
     * @param mode osu模式
     * @return Lazer成绩列表
     */
    public List<ScoreLazerDTO> extractRecentScoreList(Integer playerId, Integer type, Integer limit ,String mode)
    {
        try{
            List<ScoreLazerDTO> scoreLazerDTOS =  apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfRecentCommand(playerId,type,limit,mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    new TypeReference<List<ScoreLazerDTO>>() {});
            if(scoreLazerDTOS==null|| scoreLazerDTOS.isEmpty()) throw new LazybotRuntimeException("小妹妹打都没打在这查哪个成绩呢");
            return scoreLazerDTOS;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("小妹妹打都没打在这查哪个成绩呢");
        }
    }


    public BeatmapUserScoreLazer extractBeatmapUserScore(String beatmapId, Integer playerId, String mode, String modCombination)
    {
        try{
            BeatmapUserScoreLazer beatmapUserScoreLazer;
            if (modCombination==null || modCombination.isEmpty()) {
                beatmapUserScoreLazer = apiRequestExecutor.execute(
                        URLBuildUtil.buildURLOfBeatmapScore(beatmapId, String.valueOf(playerId),mode),
                        HTTPTypeEnum.GET,
                        TokenMonitor.getToken(),
                        null,
                        BeatmapUserScoreLazer.class);
            }
            else {
                List<String> modsArray = OsuMod.getAllModAcronym(modCombination);
                beatmapUserScoreLazer = apiRequestExecutor.execute(
                        URLBuildUtil.buildURLOfBeatmapScore(beatmapId, String.valueOf(playerId),modsArray,mode),
                        HTTPTypeEnum.GET,
                        TokenMonitor.getToken(),
                        null,
                        BeatmapUserScoreLazer.class);
            }
            if(beatmapUserScoreLazer==null||beatmapUserScoreLazer.getScore()==null)
                throw new LazybotRuntimeException("没找到" + playerId + "在" + beatmapId +"上的成绩，" + " 模式为" + mode);
            return beatmapUserScoreLazer;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("没找到" + playerId + "在" + beatmapId +"上的成绩，" + " 模式为" + mode);
        }
    }

    public List<ScoreLazerDTO> extractBeatmapUserScoreAll(Integer beatmapId, Integer playerId, String mode)
    {
        try{
            return apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfBeatmapScoreAll(String.valueOf(beatmapId), String.valueOf(playerId),mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    BeatmapUserScores.class).getScores();
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("没有找到" + playerId +"在" + beatmapId+ "上的成绩");
        }

    }

    public BeatmapDTO extractBeatmap(String beatmapId, String mode)
    {
        try{
            BeatmapDTO beatmapDTO = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfBeatmap(String.valueOf(beatmapId),mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    BeatmapDTO.class);
            if(beatmapDTO.getId()==null) {
                throw new LazybotRuntimeException("找不到" + beatmapId + "在" +mode + "模式的地图");
            }
            return beatmapDTO;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("找不到" + beatmapId + "在" +mode + "模式的地图");
        }
    }
    public List<ScoreLazerDTO> extractUserBestScoreList(String playerId, Integer offset , String mode)
    {
        try{
            List<ScoreLazerDTO> scoreLazerDTOS =  apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfUserBest(String.valueOf(playerId), offset, mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    new TypeReference<List<ScoreLazerDTO>>() {});
            if(scoreLazerDTOS==null|| scoreLazerDTOS.isEmpty()) {
                throw new LazybotRuntimeException("没这成绩: " +"Index=" + offset+1 + " PlayerID=" + playerId);
            }
            return scoreLazerDTOS;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("没这成绩: " +"Index=" + offset+1 + " PlayerID=" + playerId);
        }
    }


    public List<ScoreLazerDTO> extractUserBestScoreList(String playerId, Integer limit , Integer offset, String mode)
    {
       try{
           List<ScoreLazerDTO> scoreLazerDTOS = apiRequestExecutor.execute(
                   URLBuildUtil.buildURLOfUserBest(String.valueOf(playerId), limit, offset, mode),
                   HTTPTypeEnum.GET,
                   TokenMonitor.getToken(),
                   null,
                   new TypeReference<List<ScoreLazerDTO>>() {});
           if(scoreLazerDTOS==null|| scoreLazerDTOS.isEmpty()) {
               throw new LazybotRuntimeException("没这成绩: " +"index=" + (offset+1) + " player=" + playerId + " mode=" +mode);
           }
           return scoreLazerDTOS;
       }
       catch (NotFoundException e) {
           throw new LazybotRuntimeException("没这成绩: " +"index=" + (offset+1) + " player=" + playerId + " mode=" +mode);
       }
    }
    public List<ScoreLazerDTO> extractUserBestAll(String playerId, String mode)
    {
        try{
            List<ScoreLazerDTO> scoreLazerDTOS = apiRequestExecutor.execute(
                    URLBuildUtil.buildURLOfUserBest(String.valueOf(playerId), 100, 0, mode),
                    HTTPTypeEnum.GET,
                    TokenMonitor.getToken(),
                    null,
                    new TypeReference<List<ScoreLazerDTO>>() {}
            );
            if (scoreLazerDTOS == null || scoreLazerDTOS.isEmpty()) {
                throw new NotFoundException("找不到"+playerId+"的成绩");
            }
            if (scoreLazerDTOS.size() < 110) {
                scoreLazerDTOS.addAll(apiRequestExecutor.execute(
                        URLBuildUtil.buildURLOfUserBest(String.valueOf(playerId), 200-scoreLazerDTOS.size(), scoreLazerDTOS.size(), mode),
                        HTTPTypeEnum.GET,
                        TokenMonitor.getToken(),
                        null,
                        new TypeReference<List<ScoreLazerDTO>>() {}));
            }
            return scoreLazerDTOS;
        }
        catch (NotFoundException e) {
            throw new LazybotRuntimeException("找不到"+playerId+"的成绩");
        }
    }

    public Integer extractRankByPP(String mode, Double pp)
    {
        String rankStr = apiRequestExecutor.execute(
                URLBuildUtil.buildURLOfPpRank(OsuMode.getMode(mode).getValue(), (int) Math.round(pp)),
                HTTPTypeEnum.GET,
                null,
                null);
        return Integer.parseInt(rankStr);
    }



}
