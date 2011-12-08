/*
 * Copyright (c) 2005-2011 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */
package org.mifos.platform.rest.controller;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.mifos.accounts.api.AccountService;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.accounts.loan.persistance.LoanDao;
import org.mifos.accounts.servicefacade.AccountServiceFacade;
import org.mifos.application.master.util.helpers.PaymentTypes;
import org.mifos.application.servicefacade.LoanAccountServiceFacade;
import org.mifos.core.MifosRuntimeException;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.personnel.persistence.PersonnelDao;
import org.mifos.dto.domain.AccountPaymentParametersDto;
import org.mifos.dto.domain.AccountReferenceDto;
import org.mifos.dto.domain.CustomerDto;
import org.mifos.dto.domain.LoanInstallmentDetailsDto;
import org.mifos.dto.domain.LoanRepaymentScheduleItemDto;
import org.mifos.dto.domain.PaymentTypeDto;
import org.mifos.dto.domain.UserReferenceDto;
import org.mifos.dto.screen.LoanInformationDto;
import org.mifos.dto.screen.RepayLoanDto;
import org.mifos.dto.screen.RepayLoanInfoDto;
import org.mifos.framework.util.helpers.Money;
import org.mifos.security.MifosUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoanAccountRESTController {
	private static final ResourceBundle accountsUIResource = ResourceBundle.getBundle("org.mifos.config.localizedResources.accountsUIResources");
	
    @Autowired
    private AccountService accountService;

    @Autowired
    private LoanAccountServiceFacade loanAccountServiceFacade;
    
    @Autowired
    private AccountServiceFacade accountServiceFacade;

    @Autowired
    private LoanDao loanDao;

    @Autowired
    private PersonnelDao personnelDao;

    @RequestMapping(value = "/account/loan/repay/num-{globalAccountNum}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> repay(@PathVariable String globalAccountNum, HttpServletRequest request) throws Exception {

        String amountString = request.getParameter("amount");
        BigDecimal amount = new BigDecimal(amountString);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MifosRuntimeException("Amount must be greater than 0");
        }

        MifosUser user = (MifosUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserReferenceDto userDto = new UserReferenceDto((short) user.getUserId());
        LoanBO loan = loanDao.findByGlobalAccountNum(globalAccountNum);

        Money outstandingBeforePayment = loan.getLoanSummary().getOutstandingBalance();

        AccountReferenceDto accountDto = new AccountReferenceDto(loan.getAccountId());

        PaymentTypeDto paymentType = accountService.getLoanPaymentTypes().get(0);

        DateTime today = new DateTime();
        LocalDate receiptDate = today.toLocalDate();

        // from where these parameter should come?
        String receiptId = "";

        CustomerBO client = loan.getCustomer();
        CustomerDto customer = new CustomerDto();
        customer.setCustomerId(client.getCustomerId());
        AccountPaymentParametersDto payment = new AccountPaymentParametersDto(userDto, accountDto, amount, receiptDate,
                paymentType, globalAccountNum, receiptDate, receiptId, customer);

        accountService.makePayment(payment);


        Map<String, String> map = new HashMap<String, String>();
        map.put("status", "success");
        map.put("clientName", client.getDisplayName());
        map.put("clientNumber", client.getGlobalCustNum());
        map.put("loanDisplayName", loan.getLoanOffering().getPrdOfferingName());
        map.put("paymentDate", today.toLocalDate().toString());
        map.put("paymentTime", today.toLocalTime().toString());
        map.put("paymentAmount", loan.getLastPmnt().getAmount().toString());
        map.put("paymentMadeBy", personnelDao.findPersonnelById((short) user.getUserId()).getDisplayName());
        map.put("outstandingBeforePayment", outstandingBeforePayment.toString());
        map.put("outstandingAfterPayment", loan.getLoanSummary().getOutstandingBalance().toString());
        return map;
    }

    @RequestMapping(value = "/account/loan/fullrepay/num-{globalAccountNum}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> fullRepay(@PathVariable String globalAccountNum, HttpServletRequest request) throws Exception {
    	
    	boolean waiverInterest = Boolean.parseBoolean(request.getParameter("waiveInterest"));
    	
    	MifosUser user = (MifosUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	
        RepayLoanDto repayLoanDto = this.loanAccountServiceFacade.retrieveLoanRepaymentDetails(globalAccountNum);
        LoanBO loan = this.loanDao.findByGlobalAccountNum(globalAccountNum);
        
    	DateTime today = new DateTime();
        Date receiptDate = new Date(today.toDate().getTime());

        // from where these parameter should come?
        String receiptId = "";
        
        BigDecimal totalRepaymentAmount = ( new Money(loan.getCurrency(), repayLoanDto.getEarlyRepaymentMoney()) ).getAmount();
        BigDecimal waivedAmount = ( new Money(loan.getCurrency(), repayLoanDto.getWaivedRepaymentMoney()) ).getAmount();
        
        String paymentTypeId = "1";
        
        RepayLoanInfoDto repayLoanInfoDto = new RepayLoanInfoDto(globalAccountNum,
        		Long.toString(totalRepaymentAmount.longValue()), receiptId,
                receiptDate, paymentTypeId, (short) user.getUserId(),
                waiverInterest,
                receiptDate,totalRepaymentAmount,waivedAmount);
        
        this.loanAccountServiceFacade.makeEarlyRepaymentWithCommit(repayLoanInfoDto);
        
        CustomerBO client = loan.getCustomer();
        Money outstandingBeforePayment = loan.getLoanSummary().getOutstandingBalance();
        
    	Map<String, String> map = new HashMap<String, String>();
    	map.put("status", "success");
        map.put("clientName", client.getDisplayName());
        map.put("clientNumber", client.getGlobalCustNum());
        map.put("loanDisplayName", loan.getLoanOffering().getPrdOfferingName());
        map.put("paymentDate", today.toLocalDate().toString());
        map.put("paymentTime", today.toLocalTime().toString());
        map.put("paymentAmount", loan.getLastPmnt().getAmount().toString());
        map.put("paymentMadeBy", personnelDao.findPersonnelById((short) user.getUserId()).getDisplayName());
        map.put("outstandingBeforePayment", outstandingBeforePayment.toString());
        map.put("outstandingAfterPayment", loan.getLoanSummary().getOutstandingBalance().toString());
    	
    	return map;
    }
    
    @RequestMapping(value = "/account/loan/disburse/num-{globalAccountNum}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> disburseLoan(@PathVariable String globalAccountNum, HttpServletRequest request) throws Exception {
    	
    	MifosUser user = (MifosUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	LoanBO loan = loanDao.findByGlobalAccountNum(globalAccountNum);
    	
    	DateTime today = new DateTime();
    	PaymentTypeDto paymentType = accountService.getLoanPaymentTypes().get(0);
    	String comment = "";
    	Short paymentTypeId = PaymentTypes.CASH.getValue();
    	
    	AccountPaymentParametersDto loanDisbursement = new AccountPaymentParametersDto(new UserReferenceDto((short)user.getUserId()), 
    			new AccountReferenceDto(loan.getAccountId()), loan.getLoanAmount().getAmount(), today.toLocalDate(), paymentType, comment);
    	this.loanAccountServiceFacade.disburseLoan(loanDisbursement, paymentTypeId);
    	
    	CustomerBO client = loan.getCustomer();
    	Money outstandingBeforeDisbursement = loan.getLoanSummary().getOutstandingBalance();
    	
    	Map<String, String> map = new HashMap<String, String>();
    	map.put("status", "success");
    	map.put("clientName", client.getDisplayName());
        map.put("clientNumber", client.getGlobalCustNum());
        map.put("loanDisplayName", loan.getLoanOffering().getPrdOfferingName());
        map.put("disbursementDate", today.toLocalDate().toString());
        map.put("disbursementTime", today.toLocalTime().toString());
        map.put("disbursementAmount", loan.getLastPmnt().getAmount().toString());
        map.put("disbursementMadeBy", personnelDao.findPersonnelById((short) user.getUserId()).getDisplayName());
        map.put("outstandingBeforeDisbursement", outstandingBeforeDisbursement.toString());
        map.put("outstandingAfterDisbursement", loan.getLoanSummary().getOutstandingBalance().toString());
    	
    	return map;
    }
    
    @RequestMapping(value = "/account/loan/adjustment/num-{globalAccountNum}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> applyAdjustment(@PathVariable String globalAccountNum, HttpServletRequest request) throws Exception {
    	String note = request.getParameter("note");
    	
        if (note == null || note.isEmpty()){
        	throw new MifosRuntimeException("Note is not specified");
        }
    	
		MifosUser user = (MifosUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	LoanBO loan = loanDao.findByGlobalAccountNum(globalAccountNum);
    	CustomerBO client = loan.getCustomer();
    	
		String adjustmentAmount = loan.getLastPmnt().getAmount().toString();
		String outstandingBeforeAdjustment = loan.getLoanSummary().getOutstandingBalance().toString();
		
		try{
			accountServiceFacade.applyAdjustment(globalAccountNum, note, (short)user.getUserId());
		} catch (MifosRuntimeException e){
        	String error = accountsUIResource.getString(e.getCause().getLocalizedMessage());
        	throw new MifosRuntimeException(error);
		}
		
		DateTime today = new DateTime();
		
		Map<String, String> map = new HashMap<String, String>();
    	
		map.put("status", "success");
		map.put("clientName", client.getDisplayName());
        map.put("clientNumber", client.getGlobalCustNum());
        map.put("loanDisplayName", loan.getLoanOffering().getPrdOfferingName());
        map.put("adjustmentDate", today.toLocalDate().toString());
        map.put("adjustmentTime", today.toLocalTime().toString());
        map.put("adjustmentAmount", adjustmentAmount);
        map.put("adjustmentMadeBy", personnelDao.findPersonnelById((short) user.getUserId()).getDisplayName());
        map.put("outstandingBeforeAdjustment", outstandingBeforeAdjustment);
        map.put("outstandingAfterAdjustment", loan.getLoanSummary().getOutstandingBalance().toString());
        map.put("note", note);
		
    	return map;
    }
    
    @RequestMapping(value = "/account/loan/num-{globalAccountNum}", method = RequestMethod.GET)
    public @ResponseBody
    LoanInformationDto getLoanByNumber(@PathVariable String globalAccountNum) throws Exception {
        return loanAccountServiceFacade.retrieveLoanInformation(globalAccountNum);
    }

    @RequestMapping(value = "/account/loan/installment/num-{globalAccountNum}", method = RequestMethod.GET)
    public @ResponseBody
    LoanInstallmentDetailsDto getLoanInstallmentByNumber(@PathVariable String globalAccountNum) throws Exception {
        LoanBO loan = loanDao.findByGlobalAccountNum(globalAccountNum);
        return loanAccountServiceFacade.retrieveInstallmentDetails(loan.getAccountId());
    }

    @RequestMapping(value = "/account/loan/schedule/num-{globalAccountNum}", method = RequestMethod.GET)
    public @ResponseBody
    List<LoanRepaymentScheduleItemDto> getLoanRepaymentScheduleByNumber(@PathVariable String globalAccountNum) throws Exception {
        return loanAccountServiceFacade.retrieveLoanRepaymentSchedule(globalAccountNum);
    }
}
