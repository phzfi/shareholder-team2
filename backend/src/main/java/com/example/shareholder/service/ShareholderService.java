package com.example.shareholder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.shareholder.model.Shareholder;
import com.example.shareholder.repository.ShareholderRepository;

@Service
public class ShareholderService {

  @Autowired
  private ShareholderRepository shareholderRepository;
 
  public List<Shareholder> getShareholders() {
    return shareholderRepository.findAll();
  }
}
