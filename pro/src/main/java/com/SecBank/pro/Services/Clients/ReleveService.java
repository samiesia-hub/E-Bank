package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Releve;

public interface ReleveService {

    Releve telechargerReleve(Long compteId, String periode);
}
