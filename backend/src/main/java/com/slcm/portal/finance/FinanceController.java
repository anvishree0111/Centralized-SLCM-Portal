package com.slcm.portal.finance;

import com.slcm.portal.finance.FinanceDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FinanceController {
    private final FinanceService s;
    FinanceController(FinanceService s){this.s=s;}
    @GetMapping("/document-types") public List<DocumentTypeView> types(){return s.types(false);}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/document-requests") public List<DocumentView> docs(){return s.docs(false);}
    @PreAuthorize("hasRole('STUDENT')") @PostMapping("/student/document-requests") public DocumentView request(@Valid @RequestBody DocumentInput x){return s.request(x);}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/document-requests/{id}") public DocumentView doc(@PathVariable long id){return s.doc(id,false);}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/document-requests/{id}/download") public ResponseEntity<ByteArrayResource> download(@PathVariable long id){return file(s.download(id));}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/fees") public FeeSummary fees(@RequestParam(required=false) Long semesterId){return s.fees(semesterId);}
    @PreAuthorize("hasRole('STUDENT')") @PostMapping("/student/fees/{feeId}/payments") public PaymentView pay(@PathVariable long feeId,@Valid @RequestBody PaymentInput x){return s.submit(feeId,x);}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/payments") public List<PaymentView> payments(){return s.payments(false);}
    @PreAuthorize("hasRole('STUDENT')") @GetMapping("/student/payments/{id}/receipt") public ResponseEntity<ByteArrayResource> receipt(@PathVariable long id){return file(s.receipt(id));}

    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/document-types") public List<DocumentTypeView> allTypes(){return s.types(true);}
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/document-types") public DocumentTypeView createType(@Valid @RequestBody DocumentTypeInput x){return s.createType(x);}
    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/document-types/{id}") public DocumentTypeView editType(@PathVariable long id,@Valid @RequestBody DocumentTypeInput x){return s.editType(id,x);}
    @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/admin/document-types/{id}") public void deleteType(@PathVariable long id){s.deleteType(id);}
    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/document-requests") public List<DocumentView> allDocs(){return s.docs(true);}
    @PreAuthorize("hasRole('ADMIN')") @PutMapping(value="/admin/document-requests/{id}/status",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public DocumentView review(@PathVariable long id,@Valid @ModelAttribute DocumentReview x,@RequestPart(required=false) MultipartFile file){return s.doc(id,true,x,file);}
    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/fees") public FeeSummary allFees(){return s.allFees();}
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/fees") public FeeView createFee(@Valid @RequestBody FeeInput x){return s.createFee(x);}
    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/fees/{id}") public FeeView updateFee(@PathVariable long id,@Valid @RequestBody FeeInput x){return s.updateFee(id,x);}
    @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/admin/fees/{id}") public void deleteFee(@PathVariable long id){s.deleteFee(id);}
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/payments") public PaymentView createPayment(@Valid @RequestBody AdminPaymentInput x){return s.adminPayment(x);}
    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/payments") public List<PaymentView> allPayments(){return s.payments(true);}
    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/payments/{id}/verify") public PaymentView verify(@PathVariable long id){return s.verify(id,true);}
    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/payments/{id}/reject") public PaymentView reject(@PathVariable long id){return s.verify(id,false);}
    private static ResponseEntity<ByteArrayResource> file(Download d){return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.filename()).build().toString()).contentLength(d.bytes().length).body(new ByteArrayResource(d.bytes()));}
}
