package com.nextlabs.rms.cc.service;

import com.nextlabs.rms.cc.pojos.ControlCenterObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterObligationParameters;

import java.util.ArrayList;
import java.util.List;

public class ControlCenterObligationService {

    static final boolean IS_EDITABLE = true;
    static final String OBLIGATION_NAME = "OB_OVERLAY";
    static final String TEXT = "Text";

    ControlCenterObligationService() {

    }

    public ControlCenterObligation getWatermarkPolicyModelObligation() {
        ControlCenterObligation obligation = new ControlCenterObligation();
        obligation.setName(OBLIGATION_NAME);
        obligation.setShortName(OBLIGATION_NAME);
        obligation.setRunAt("PEP");

        List<ControlCenterObligationParameters> parametersList = new ArrayList<>();
        String textValue = "$(User)\\n$(Date)$(Time)";
        ControlCenterObligationParameters textParameter = getControlCenterObligationParameters(textValue, ControlCenterObligationParameters.ControlCenterObligationParamType.SINGLE_ROW.getTypeValue(), null);
        parametersList.add(textParameter);

        ControlCenterObligationParameters[] controlCenterObligationParameters = new ControlCenterObligationParameters[parametersList.size()];
        int count = 0;
        for (ControlCenterObligationParameters parameter : parametersList) {
            controlCenterObligationParameters[count] = parameter;
            count++;
        }
        obligation.setParameters(controlCenterObligationParameters);
        return obligation;
    }

    private ControlCenterObligationParameters getControlCenterObligationParameters(String value, String type,
        String listValue) {
        ControlCenterObligationParameters parameter = new ControlCenterObligationParameters();
        parameter.setName(ControlCenterObligationService.TEXT);
        parameter.setShortName(ControlCenterObligationService.TEXT);
        parameter.setType(type);

        if (type.equals(ControlCenterObligationParameters.ControlCenterObligationParamType.SINGLE_ROW.getTypeValue())) {
            parameter.setDefaultValue(value);
        }
        if (type.equals(ControlCenterObligationParameters.ControlCenterObligationParamType.LIST.getTypeValue())) {
            parameter.setDefaultValue(listValue.substring(0, listValue.indexOf(',')));
            parameter.setListValues(null);
        }
        parameter.setHidden(false);
        parameter.setEditable(ControlCenterObligationService.IS_EDITABLE);
        parameter.setMandatory(false);
        parameter.setSortOrder(0);
        return parameter;
    }
}
