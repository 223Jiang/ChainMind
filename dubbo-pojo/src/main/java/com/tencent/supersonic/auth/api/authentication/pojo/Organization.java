package com.tencent.supersonic.auth.api.authentication.pojo;

import com.beust.jcommander.internal.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Organization {

    private String id;

    private String parentId;

    private String name;

    private String fullName;

    private List<Organization> subOrganizations = Lists.newArrayList();

    private boolean isRoot;
}