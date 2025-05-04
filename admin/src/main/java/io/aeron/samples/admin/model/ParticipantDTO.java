package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class ParticipantDTO implements Serializable
{
    private final long participantId;
    private final String name;
}
