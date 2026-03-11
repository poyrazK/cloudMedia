package com.cloudmedia.content.api.channel.dto;

import com.cloudmedia.content.persistence.entity.ChannelMemberRole;

public record UserChannelResponse(ChannelResponse channel, ChannelMemberRole role) {
}
