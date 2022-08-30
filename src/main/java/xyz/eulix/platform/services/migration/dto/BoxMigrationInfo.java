package xyz.eulix.platform.services.migration.dto;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 盒子割接信息
 */
@Data
public class BoxMigrationInfo {
    @Schema(description = "network client id；传参为空时将由平台重新生成")
    private String networkClientId;

    @Valid
    @NotEmpty
    @Schema(description = "用户列表")
    private List<UserMigrationInfo> userInfos;
}
