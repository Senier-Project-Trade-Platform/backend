package com.sptp.backend.member.web.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistSaveResponseDto {

    private String nickname;
    private String userId;
    private String email;
    private String telephone;

    private String education;
    private String history;
    private String description;
    private String instagram;
    private String behance;
}