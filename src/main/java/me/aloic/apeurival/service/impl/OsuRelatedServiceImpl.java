//package me.aloic.apeurival.service.impl;
//
//import com.alibaba.fastjson2.JSON;
//import me.aloic.apeurival.entity.dto.osu.player.PlayerInfoDTO;
//import me.aloic.apeurival.service.OsuRelatedService;
//
//import java.util.Objects;
//
//public class OsuRelatedServiceImpl implements OsuRelatedService
//{
//    @Override
//    public PerformancePlusProfile getUserPerformancePlusProfile(Long userId)
//    {
//        try{
//            PlayerInfoDTO info = OsuToolsUtil.setupPlayerInfoVO(getTargetPlayerInfoDTO(params));
//            playerInfoVO.setMode(params.getMode());
//            if (playerInfoVO.getPrimaryColor()==333) playerInfoVO.setPrimaryColor(208);
//            PPPlusPerformance performance=dataExtractor.extractPerformancePlusPlayerTotal(playerInfoVO.getId());
//
//            return new PerformancePlusProfile(performance,playerInfoVO);
//        }
//        catch (LazybotRuntimeException e) {
//            throw e;
//        }
//        catch (Exception e){
//            logger.error("Pp+服务正在维护或生成失败，请稍后再试.params:{}", JSON.toJSONString(params), e);
//            throw new LazybotRuntimeException("Pp+服务正在维护或生成失败，请稍后再试");
//        }
//
//    }
//
//}
