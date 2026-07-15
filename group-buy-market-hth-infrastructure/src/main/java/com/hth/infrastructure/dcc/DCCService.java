package com.hth.infrastructure.dcc;

import com.hth.types.annotations.DCCValue;
import com.hth.types.common.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DCCService {

    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    @DCCValue("cutRange:100")
    private String cutRange;

    @DCCValue("whiteList:xiaofuge,hth,hth01,hth02")
    private String whiteList;

    @DCCValue("scBlacklist:s02c02")
    private String scBlacklist;

    public boolean isDowngradeSwitch(){
        return "1".equals(downgradeSwitch);
    }
    public boolean isCutRange(String userId){
        int hashCode = Math.abs(userId.hashCode());
        int lastTwoDigits = hashCode % 100;
        if(lastTwoDigits <= Integer.parseInt(cutRange)){
            return true;
        }
        return false;
    }
    public boolean isWhiteList(String userId){
        return whiteList.contains(userId);
    }

    public boolean isSCBlackIntercept(String source, String channel) {
        List<String> list = Arrays.asList(scBlacklist.split(Constants.SPLIT));
        return list.contains(source + channel);
    }
}
