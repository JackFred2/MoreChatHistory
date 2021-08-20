package red.jackf.morechathistory;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "morechathistory")
public class MoreChatHistoryConfig implements ConfigData {
    int maxHistory = 4096;

    @Override
    public void validatePostLoad() {
        maxHistory = Math.max(maxHistory, 100);
    }
}

