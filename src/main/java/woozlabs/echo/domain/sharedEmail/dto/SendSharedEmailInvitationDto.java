package woozlabs.echo.domain.sharedEmail.dto;

import lombok.*;
import woozlabs.echo.domain.sharedEmail.entity.Access;
import woozlabs.echo.domain.sharedEmail.entity.Permission;
import woozlabs.echo.domain.sharedEmail.entity.SharedDataType;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendSharedEmailInvitationDto {

    private String dataId;
    private String invitationMemo;
    private Access access;
    private Permission permission;
    private SharedDataType sharedDataType;
    private boolean notifyInvitation;
    private List<String> invitees;
}
